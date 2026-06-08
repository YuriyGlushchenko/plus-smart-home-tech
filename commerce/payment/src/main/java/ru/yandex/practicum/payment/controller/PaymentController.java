package ru.yandex.practicum.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.api.PaymentApi;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.payment.service.PaymentService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    @PostMapping("/productCost")
    public Double productCost(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/payment/productCost - Расчёт стоимости товаров для заказа: {}",
                order.getOrderId());
        return paymentService.productCost(order);
    }

    @PostMapping("/totalCost")
    public Double getTotalCost(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/payment/totalCost - Расчёт полной стоимости заказа: {}",
                order.getOrderId());
        return paymentService.getTotalCost(order);
    }

    @PostMapping
    public PaymentDto payment(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/payment - Формирование оплаты для заказа: {}", order.getOrderId());
        return paymentService.payment(order);
    }

    @PostMapping("/refund")
    public void paymentSuccess(@RequestBody UUID paymentId) {
        log.debug("POST /api/v1/payment/refund - Успешная оплата для платежа: {}", paymentId);
        paymentService.paymentSuccess(paymentId);
    }

    @PostMapping("/failed")
    public void paymentFailed(@RequestBody UUID paymentId) {
        log.debug("POST /api/v1/payment/failed - Ошибка оплаты для платежа: {}", paymentId);
        paymentService.paymentFailed(paymentId);
    }
}