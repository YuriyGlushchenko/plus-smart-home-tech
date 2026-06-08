package ru.yandex.practicum.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.api.OrderApi;

@FeignClient(name = "order")
public interface OrderClient extends OrderApi {
}
