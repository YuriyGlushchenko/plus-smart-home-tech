package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.exceptions.exceptions.NoOrderFoundException;
import ru.yandex.practicum.exceptions.exceptions.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.payment.client.OrderClient;
import ru.yandex.practicum.payment.client.ShoppingStoreClient;

import ru.yandex.practicum.payment.mapper.PaymentMapper;
import ru.yandex.practicum.payment.model.Payment;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.repository.PaymentRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    @Override
    public Double productCost(OrderDto order) {
        log.debug("Расчёт стоимости товаров для заказа: {}", order.getOrderId());

        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException("Нет товаров для расчёта стоимости");
        }

        double totalProductCost = 0.0;

        for (Map.Entry<UUID, Integer> entry : order.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            try {
                ProductDto product = shoppingStoreClient.getProduct(productId);
                totalProductCost += product.getPrice().doubleValue() * quantity;
                log.debug("Товар {}: цена={}, количество={}, сумма={}",
                        productId, product.getPrice(), quantity, product.getPrice().doubleValue() * quantity);
            } catch (Exception e) {
                log.error("Не удалось получить цену товара {}: {}", productId, e.getMessage());
                throw new NotEnoughInfoInOrderToCalculateException(
                        "Не удалось получить информацию о товаре " + productId);
            }
        }

        log.debug("Общая стоимость товаров для заказа {}: {}", order.getOrderId(), totalProductCost);
        return totalProductCost;
    }

    @Override
    public Double getTotalCost(OrderDto order) {
        log.debug("Расчёт полной стоимости заказа: {}", order.getOrderId());

        if (order.getProductPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Не указана стоимость товаров");
        }

        if (order.getDeliveryPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Не указана стоимость доставки");
        }

        // a. НДС = 10% от стоимости товаров
        double vat = order.getProductPrice() * 0.1;
        log.debug("НДС (10% от стоимости товаров): {}", vat);

        // b. Стоимость товаров с НДС
        double productPriceWithVat = order.getProductPrice() + vat;
        log.debug("Стоимость товаров с НДС: {}", productPriceWithVat);

        // c. Добавляем стоимость доставки
        double totalCost = productPriceWithVat + order.getDeliveryPrice();
        log.debug("Полная стоимость заказа {}: {}", order.getOrderId(), totalCost);

        return totalCost;
    }

    @Override
    @Transactional
    public PaymentDto payment(OrderDto order) {
        log.debug("Формирование оплаты для заказа: {}", order.getOrderId());

        if (order.getProductPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Не указана стоимость товаров");
        }

        if (order.getDeliveryPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Не указана стоимость доставки");
        }

        if (order.getTotalPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Не указана общая стоимость");
        }

        // Проверяем, нет ли уже оплаты для этого заказа
        if (paymentRepository.findByOrderId(order.getOrderId()).isPresent()) {
            log.warn("Оплата для заказа {} уже существует", order.getOrderId());
            throw new IllegalStateException("Оплата для этого заказа уже создана");
        }

        // Расчёт НДС (10% от стоимости товаров)
        double vat = order.getProductPrice() * 0.1;

        // Создаём запись об оплате со статусом PENDING
        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .productPrice(order.getProductPrice())
                .deliveryPrice(order.getDeliveryPrice())
                .totalPrice(order.getTotalPrice())
                .feeTotal(vat)
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.debug("Создана запись об оплате {} для заказа {} со статусом PENDING",
                savedPayment.getId(), savedPayment.getOrderId());

        // Эмуляция асинхронного ответа от платёжного шлюза
        emulatePaymentGateway(savedPayment.getId());

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional
    public void paymentSuccess(UUID paymentId) {
        log.debug("Успешная оплата для платежа: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException("Платёж с id " + paymentId + " не найден"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Платёж {} уже обработан, текущий статус: {}", paymentId, payment.getStatus());
            throw new IllegalStateException("Платёж уже обработан");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
        log.debug("Статус платежа {} изменён на SUCCESS", paymentId);

        // Уведомляем сервис заказов об успешной оплате
        try {
            orderClient.paymentSuccess(payment.getOrderId());
            log.debug("Сервис заказов уведомлён об успешной оплате заказа {}", payment.getOrderId());
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об успешной оплате: {}", e.getMessage());
            // Не бросаем исключение, чтобы не откатывать транзакцию
        }
    }

    @Override
    @Transactional
    public void paymentFailed(UUID paymentId) {
        log.debug("Ошибка оплаты для платежа: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException("Платёж с id " + paymentId + " не найден"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Платёж {} уже обработан, текущий статус: {}", paymentId, payment.getStatus());
            throw new IllegalStateException("Платёж уже обработан");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        log.debug("Статус платежа {} изменён на FAILED", paymentId);

        // Уведомляем сервис заказов об ошибке оплаты
        try {
            orderClient.paymentFailed(payment.getOrderId());
            log.debug("Сервис заказов уведомлён об ошибке оплаты заказа {}", payment.getOrderId());
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об ошибке оплаты: {}", e.getMessage());
        }
    }

    // ========== Private методы ==========

    private void emulatePaymentGateway(UUID paymentId) {
        // Эмуляция асинхронного ответа от платёжного шлюза
        // В реальной системе здесь был бы callback от внешнего сервиса
        CompletableFuture.runAsync(() -> {
            try {
                // Симулируем задержку обработки платежа (1-3 секунды)
                long delay = 1000 + (long) (Math.random() * 2000);
                Thread.sleep(delay);

                // 80% успешных платежей, 20% неудачных (для тестирования)
                boolean success = Math.random() < 0.8;

                log.debug("Эмуляция ответа от платёжного шлюза для платежа {}: {}",
                        paymentId, success ? "SUCCESS" : "FAILED");

                if (success) {
                    paymentSuccess(paymentId);
                } else {
                    paymentFailed(paymentId);
                }
            } catch (InterruptedException e) {
                log.error("Ошибка при эмуляции платежного шлюза: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }
}