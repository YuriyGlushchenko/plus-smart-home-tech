package ru.yandex.practicum.payment.service;

import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;

import java.util.UUID;

public interface PaymentService {

    Double productCost(OrderDto order);

    Double getTotalCost(OrderDto order);

    PaymentDto payment(OrderDto order);

    void paymentSuccess(UUID paymentId);

    void paymentFailed(UUID paymentId);
}