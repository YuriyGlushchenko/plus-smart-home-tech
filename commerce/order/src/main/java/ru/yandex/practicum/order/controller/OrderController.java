package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.api.OrderApi;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ProductReturnRequest;
import ru.yandex.practicum.order.service.OrderService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> getClientOrders(@RequestParam String username) {
        log.debug("GET /api/v1/order - Получение заказов пользователя: {}", username);
        return orderService.getClientOrders(username);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createNewOrder(@Valid @RequestBody CreateNewOrderRequest request) {
        log.debug("PUT /api/v1/order - Создание нового заказа");
        String username = extractUsername(); // TODO: получать из security context
        return orderService.createNewOrder(username, request);
    }

    @PostMapping("/return")
    public OrderDto productReturn(@Valid @RequestBody ProductReturnRequest request) {
        log.debug("POST /api/v1/order/return - Возврат товаров");
        return orderService.productReturn(request);
    }

    @PostMapping("/payment")
    public OrderDto payment(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/payment - Оплата заказа: {}", orderId);
        return orderService.payment(orderId);
    }

    @PostMapping("/payment/failed")
    public OrderDto paymentFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/payment/failed - Ошибка оплаты заказа: {}", orderId);
        return orderService.paymentFailed(orderId);
    }

    @PostMapping("/payment/success")
    public OrderDto paymentSuccess(@RequestBody UUID orderId) {
        log.debug("Уведомление об успешной оплате заказа: {}", orderId);
        return orderService.paymentSuccess(orderId);
    }

    @PostMapping("/delivery")
    public OrderDto delivery(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/delivery - Доставка заказа: {}", orderId);
        return orderService.delivery(orderId);
    }

    @PostMapping("/delivery/failed")
    public OrderDto deliveryFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/delivery/failed - Ошибка доставки заказа: {}", orderId);
        return orderService.deliveryFailed(orderId);
    }

    @PostMapping("/completed")
    public OrderDto complete(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/completed - Завершение заказа: {}", orderId);
        return orderService.complete(orderId);
    }

    @PostMapping("/calculate/total")
    public OrderDto calculateTotalCost(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/calculate/total - Расчёт общей стоимости: {}", orderId);
        return orderService.calculateTotalCost(orderId);
    }

    @PostMapping("/calculate/delivery")
    public OrderDto calculateDeliveryCost(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/calculate/delivery - Расчёт стоимости доставки: {}", orderId);
        return orderService.calculateDeliveryCost(orderId);
    }

    @PostMapping("/assembly")
    public OrderDto assembly(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/assembly - заказ собран: {}", orderId);
        return orderService.assemblySuccess(orderId);
    }

    @PostMapping("/assembly/failed")
    public OrderDto assemblyFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/assembly/failed - Ошибка сборки заказа: {}", orderId);
        return orderService.assemblyFailed(orderId);
    }

    // Временный метод для получения username
    private String extractUsername() {
        // TODO: получить из Spring Security
        return "temp_user@example.com";
    }
}