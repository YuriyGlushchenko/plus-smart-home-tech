package ru.yandex.practicum.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.api.DeliveryApi;
import ru.yandex.practicum.dto.AddressDto;

@FeignClient(name = "delivery")
public interface DeliveryClient extends DeliveryApi {

}