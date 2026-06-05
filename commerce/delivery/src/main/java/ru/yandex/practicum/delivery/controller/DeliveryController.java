package ru.yandex.practicum.delivery.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.api.DeliveryApi;
import ru.yandex.practicum.delivery.service.DeliveryService;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController implements DeliveryApi {

    private final DeliveryService deliveryService;

    @PutMapping
    public DeliveryDto planDelivery(@RequestBody DeliveryDto deliveryDto) {
        log.debug("PUT /api/v1/delivery - Планирование доставки для заказа: {}", deliveryDto.getOrderId());
        return deliveryService.planDelivery(deliveryDto);
    }

    @PostMapping("/cost")
    public Double deliveryCost(
            @RequestBody OrderDto order,
            @RequestParam AddressDto toAddress) {
        log.debug("POST /api/v1/delivery/cost - Расчёт стоимости доставки для заказа: {}, адрес: {}",
                order.getOrderId(), toAddress.getStreet());
        return deliveryService.deliveryCost(order, toAddress);
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