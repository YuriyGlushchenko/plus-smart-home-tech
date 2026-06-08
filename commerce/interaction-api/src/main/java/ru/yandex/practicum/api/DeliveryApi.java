package ru.yandex.practicum.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;

import java.util.UUID;

public interface DeliveryApi {

    @PutMapping("/api/v1/delivery")
    DeliveryDto planDelivery(@Valid @RequestBody DeliveryDto deliveryDto);

    @PostMapping("/api/v1/delivery/cost")
    public Double deliveryCost(@Valid @RequestBody OrderDto order, @RequestParam AddressDto toAddress);

    @PostMapping("/api/v1/delivery/picked")
    void deliveryPicked(@NotNull @RequestBody UUID orderId);

    @PostMapping("/api/v1/delivery/successful")
    void deliverySuccessful(@NotNull @RequestBody UUID orderId);

    @PostMapping("/api/v1/delivery/failed")
    void deliveryFailed(@NotNull @RequestBody UUID orderId);
}