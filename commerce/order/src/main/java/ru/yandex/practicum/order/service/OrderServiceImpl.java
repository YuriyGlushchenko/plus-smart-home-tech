package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exceptions.exceptions.NoOrderFoundException;
import ru.yandex.practicum.exceptions.exceptions.NotAuthorizedUserException;
import ru.yandex.practicum.order.client.DeliveryClient;
import ru.yandex.practicum.order.client.PaymentClient;
import ru.yandex.practicum.order.client.ShoppingCartClient;
import ru.yandex.practicum.order.client.WarehouseClient;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.model.Order;
import ru.yandex.practicum.order.model.OrderState;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ShoppingCartClient shoppingCartClient;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;

    @Override
    public List<OrderDto> getClientOrders(String username) {
        log.debug("Получение заказов для пользователя: {}", username);
        validateUsername(username);

        List<Order> orders = orderRepository.findByUsernameOrderByCreatedAtDesc(username);
        return orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDto createNewOrder(String username, CreateNewOrderRequest request) {
        log.debug("Создание нового заказа для пользователя: {}", username);
        validateUsername(username);

        BookedProductsDto booked = checkWarehouse(request.getShoppingCart());

        Order order = orderMapper.toEntity(request, username);
        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.getFragile());

        Order savedOrder = orderRepository.save(order);
        log.debug("Заказ создан с id: {}", savedOrder.getId());

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        log.debug("Расчёт стоимости доставки заказа: {}", orderId);

        Order order = findOrderById(orderId);

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
        log.debug("Адрес склада: {}", warehouseAddress.getStreet());

        AddressDto deliveryAddress = AddressDto.builder()
                .country(order.getDeliveryAddress().getCountry())
                .city(order.getDeliveryAddress().getCity())
                .street(order.getDeliveryAddress().getStreet())
                .house(order.getDeliveryAddress().getHouse())
                .flat(order.getDeliveryAddress().getFlat())
                .build();
        log.debug("Адрес доставки: {}", deliveryAddress.getStreet());

        OrderDto orderDto = orderMapper.toDto(order);
        Double deliveryCost = deliveryClient.deliveryCost(orderDto, deliveryAddress);
        log.debug("Рассчитанная стоимость доставки: {}", deliveryCost);

        order.setDeliveryPrice(deliveryCost);

        Order savedOrder = orderRepository.save(order);
        log.debug("Стоимость доставки для заказа {} сохранена: {}", orderId, deliveryCost);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        log.debug("Расчёт общей стоимости заказа: {}", orderId);

        Order order = findOrderById(orderId);

        // проверяем, посчитана ли уже стоимость товаров, если нет, то считаем
        if (order.getProductPrice() == null) {
            OrderDto orderDto = orderMapper.toDto(order);
            Double productCost = paymentClient.productCost(orderDto);
            order.setProductPrice(productCost);
        }

        // проверяем, посчитана ли уже стоимость доставки, если нет, то считаем
        if (order.getDeliveryPrice() == null) {
            order.setDeliveryPrice(calculateDeliveryCost(orderId).getDeliveryPrice());
        }

        // считаем полную стоимость с доставками и налогами
        OrderDto orderDto = orderMapper.toDto(order);
        Double totalCost = paymentClient.getTotalCost(orderDto);
        order.setTotalPrice(totalCost);

        orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderDto payment(UUID orderId) {
        log.debug("Инициализация оплаты заказа: {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getState() != OrderState.NEW && order.getState() != OrderState.ON_PAYMENT && order.getState() != OrderState.PAYMENT_FAILED) {
            throw new IllegalStateException("Заказ не может быть оплачен в текущем статусе: " + order.getState());
        }

        OrderDto orderDto = orderMapper.toDto(order);
        PaymentDto payment = paymentClient.payment(orderDto);

        order.setPaymentId(payment.getPaymentId());
        order.setState(OrderState.ON_PAYMENT);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        log.debug("Ошибка оплаты заказа: {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getState() != OrderState.ON_PAYMENT) {
            log.warn("Заказ {} не ожидает оплаты, текущий статус: {}", orderId, order.getState());
//            throw new IllegalStateException("Заказ не ожидает оплаты");
        }

        order.setState(OrderState.PAYMENT_FAILED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto paymentSuccess(UUID orderId) {
        log.debug("Успешная оплата заказа: {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getState() != OrderState.ON_PAYMENT) {
            log.warn("Заказ {} не ожидает оплаты, текущий статус: {}", orderId, order.getState());
//            throw new IllegalStateException("Заказ не ожидает оплаты");
        }

        order.setState(OrderState.PAID);
        Order savedOrder = orderRepository.save(order);

        // раз оплата успешна, начинаем сборку заказа
        startAssembly(savedOrder);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto assemblySuccess(UUID orderId) {
        // видимо, вызывается из сервиса доставки "также необходимо изменить статус заказа на ASSEMBLED в сервисе заказов"
        log.debug("Успешная сборка заказа: {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getState() != OrderState.PAID) {
            log.warn("Заказ {} не в статусе PAID, текущий статус: {}", orderId, order.getState());
            throw new IllegalStateException("Заказ не оплачен, после сборки не готов к доставке");
        }

        order.setState(OrderState.ASSEMBLED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        log.debug("Ошибка сборки заказа: {}", orderId);

        Order order = findOrderById(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto delivery(UUID orderId) {
        log.debug("Успешная доставка заказа: {}", orderId);

        Order order = findOrderById(orderId);
        order.setState(OrderState.DELIVERED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        log.debug("Ошибка доставки заказа: {}", orderId);

        Order order = findOrderById(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto complete(UUID orderId) {
        log.debug("Завершение заказа: {}", orderId);

        Order order = findOrderById(orderId);
        order.setState(OrderState.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        log.debug("Возврат заказа: {}", request.getOrderId());

        Order order = findOrderById(request.getOrderId());

        // Возвращаем товары на склад
        warehouseClient.acceptReturn(request.getProducts());

        order.setState(OrderState.PRODUCT_RETURNED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }



    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Имя пользователя не должно быть пустым");
        }
    }

    private Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Заказ с id " + orderId + " не найден"));
    }

    private BookedProductsDto checkWarehouse(ShoppingCartDto shoppingCart) {
        try {
            BookedProductsDto booked = warehouseClient.checkProductQuantityEnoughForShoppingCart(shoppingCart);
            log.debug("Проверка склада пройдена. Вес: {}, Объём: {}, Хрупкие: {}",
                    booked.getDeliveryWeight(), booked.getDeliveryVolume(), booked.getFragile());
            return booked;
        } catch (feign.FeignException e) {
            if (e.status() == 409) {
                throw new IllegalArgumentException("Недостаточно товаров на складе");
            } else if (e.status() == 400) {
                log.error("Ошибка валидации при проверке склада: {}", e.getMessage());
                throw new IllegalArgumentException("Неверный запрос к складу");
            } else if (e.status() == 404) {
                log.error("Сервис склада не найден: {}", e.getMessage());
                throw new RuntimeException("Сервис склада временно недоступен");
            } else {
                log.error("Ошибка при проверке склада: status={}, message={}", e.status(), e.getMessage());
                throw new RuntimeException("Ошибка при проверке наличия товаров: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Неожиданная ошибка при проверке склада: {}", e.getMessage());
            throw new RuntimeException("Невозможно проверить наличие товаров");
        }
    }

    private void startAssembly(Order order) {
        log.debug("Запуск сборки заказа: {}", order.getId());

        AssemblyProductsForOrderRequest request = AssemblyProductsForOrderRequest.builder()
                .orderId(order.getId())
                .products(order.getProducts())
                .build();

        try {
            warehouseClient.assemblyProductsForOrder(request);
            log.debug("Сборка заказа {} инициирована", order.getId());
        } catch (Exception e) {
            log.error("Ошибка при инициации сборки заказа {}: {}", order.getId(), e.getMessage());
        }
    }
}