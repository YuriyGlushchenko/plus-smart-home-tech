package ru.yandex.practicum.delivery.service;


import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;

import java.util.UUID;

public interface DeliveryService {

    DeliveryDto planDelivery(AddressDto fromAddress, AddressDto toAddress, UUID orderId,
                             Double deliveryWeight, Double deliveryVolume, Boolean fragile);

    Double deliveryCost(AddressDto fromAddress, AddressDto toAddress,
                        Double weight, Double volume, Boolean fragile);

    void deliveryPicked(UUID orderId);

    void deliverySuccessful(UUID orderId);

    void deliveryFailed(UUID orderId);
}