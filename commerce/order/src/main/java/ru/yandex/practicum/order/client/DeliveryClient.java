package ru.yandex.practicum.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.AddressDto;

@FeignClient(name = "delivery")
public interface DeliveryClient {

    @PostMapping("/api/v1/delivery/deliveryCost")
    Double deliveryCost(
            @RequestParam AddressDto from,
            @RequestParam AddressDto to,
            @RequestParam Double weight,
            @RequestParam Double volume,
            @RequestParam Boolean fragile);
}