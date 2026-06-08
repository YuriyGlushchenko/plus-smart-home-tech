package ru.yandex.practicum.delivery.service;


import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;

import java.util.UUID;

public interface DeliveryService {

    DeliveryDto planDelivery(DeliveryDto deliveryDto);

    Double deliveryCost(OrderDto orderDto, AddressDto toAddress);

    void deliveryPicked(UUID orderId);

    void deliverySuccessful(UUID orderId);

    void deliveryFailed(UUID orderId);
}