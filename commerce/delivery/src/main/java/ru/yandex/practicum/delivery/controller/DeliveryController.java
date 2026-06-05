package ru.yandex.practicum.delivery.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.api.DeliveryApi;
import ru.yandex.practicum.delivery.service.DeliveryService;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController implements DeliveryApi {

    private final DeliveryService deliveryService;

    @PutMapping
    public DeliveryDto planDelivery(
            @RequestParam AddressDto fromAddress,
            @RequestParam AddressDto toAddress,
            @RequestParam UUID orderId,
            @RequestParam Double deliveryWeight,
            @RequestParam Double deliveryVolume,
            @RequestParam Boolean fragile) {
        log.debug("PUT /api/v1/delivery - Планирование доставки для заказа: {}", orderId);
        return deliveryService.planDelivery(fromAddress, toAddress, orderId,
                deliveryWeight, deliveryVolume, fragile);
    }

    @PostMapping("/cost")
    public Double deliveryCost(
            @RequestParam AddressDto fromAddress,
            @RequestParam AddressDto toAddress,
            @RequestParam Double weight,
            @RequestParam Double volume,
            @RequestParam Boolean fragile) {
        log.debug("POST /api/v1/delivery/cost - Расчёт стоимости доставки");
        return deliveryService.deliveryCost(fromAddress, toAddress, weight, volume, fragile);
    }

    @PostMapping("/picked")
    public void deliveryPicked(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/delivery/picked - Приём товаров в доставку для заказа: {}", orderId);
        deliveryService.deliveryPicked(orderId);
    }

    @PostMapping("/successful")
    public void deliverySuccessful(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/delivery/successful - Успешная доставка для заказа: {}", orderId);
        deliveryService.deliverySuccessful(orderId);
    }

    @PostMapping("/failed")
    public void deliveryFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/delivery/failed - Ошибка доставки для заказа: {}", orderId);
        deliveryService.deliveryFailed(orderId);
    }
}