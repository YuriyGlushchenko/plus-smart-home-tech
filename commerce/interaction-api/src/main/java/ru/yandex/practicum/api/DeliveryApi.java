package ru.yandex.practicum.api;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;

import java.util.UUID;

public interface DeliveryApi {

    @PutMapping("/api/v1/delivery")
    DeliveryDto planDelivery(
            @RequestParam AddressDto fromAddress,
            @RequestParam AddressDto toAddress,
            @RequestParam UUID orderId,
            @RequestParam Double deliveryWeight,
            @RequestParam Double deliveryVolume,
            @RequestParam Boolean fragile);

    @PostMapping("/api/v1/delivery/cost")
    Double deliveryCost(
            @RequestParam AddressDto fromAddress,
            @RequestParam AddressDto toAddress,
            @RequestParam Double weight,
            @RequestParam Double volume,
            @RequestParam Boolean fragile);

    @PostMapping("/api/v1/delivery/picked")
    void deliveryPicked(@RequestBody UUID orderId);

    @PostMapping("/api/v1/delivery/successful")
    void deliverySuccessful(@RequestBody UUID orderId);

    @PostMapping("/api/v1/delivery/failed")
    void deliveryFailed(@RequestBody UUID orderId);
}