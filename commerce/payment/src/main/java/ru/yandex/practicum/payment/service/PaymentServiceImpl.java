package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.exceptions.exceptions.NoOrderFoundException;
import ru.yandex.practicum.exceptions.exceptions.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.payment.client.OrderClient;
import ru.yandex.practicum.payment.client.ShoppingStoreClient;
import ru.yandex.practicum.payment.mapper.PaymentMapper;
import ru.yandex.practicum.payment.model.Payment;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.repository.PaymentRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        List<UUID> productIds = new java.util.ArrayList<>(order.getProducts().keySet());

        // добавил эндпоинт в ShoppingStore для получения всех цен одним запросом
        Map<UUID, Double> productPrices;
        try {
            productPrices = shoppingStoreClient.getProductsPrices(productIds);
            log.trace("Получены цены для {} товаров", productPrices.size());
        } catch (Exception e) {
            log.error("Не удалось получить цены товаров: {}", e.getMessage());
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Не удалось получить информацию о товарах: " + e.getMessage());
        }

        double totalProductCost = 0.0;

        for (Map.Entry<UUID, Integer> entry : order.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            Double price = productPrices.get(productId);
            if (price == null) {
                log.error("Цена для товара {} не найдена", productId);
                throw new NotEnoughInfoInOrderToCalculateException(
                        "Не удалось получить цену для товара " + productId);
            }

            totalProductCost += price * quantity;
            log.trace("Товар {}: цена={}, количество={}, сумма={}", productId, price, quantity, price * quantity);
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

        double vat = order.getProductPrice() * 0.1;
        log.trace("НДС: {}", vat);

        double productPriceWithVat = order.getProductPrice() + vat;
        log.trace("Стоимость товаров с НДС: {}", productPriceWithVat);

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

        if (paymentRepository.findByOrderId(order.getOrderId()).isPresent()) {
            log.warn("Оплата для заказа {} уже существует", order.getOrderId());
            throw new IllegalStateException("Оплата для этого заказа уже создана");
        }

        double vat = order.getProductPrice() * 0.1;

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
//            throw new IllegalStateException("Платёж уже обработан");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
        log.trace("Статус платежа {} изменён на SUCCESS", paymentId);

        try {
            orderClient.paymentSuccess(payment.getOrderId());
            log.debug("Сервис заказов уведомлён об успешной оплате заказа {}", payment.getOrderId());
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об успешной оплате: {}", e.getMessage());
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
//            throw new IllegalStateException("Платёж уже обработан");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        log.trace("Статус платежа {} изменён на FAILED", paymentId);

        try {
            orderClient.paymentFailed(payment.getOrderId());
            log.debug("Сервис заказов уведомлён об ошибке оплаты заказа {}", payment.getOrderId());
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об ошибке оплаты: {}", e.getMessage());
        }
    }
}