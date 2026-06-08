package ru.yandex.practicum.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ProductReturnRequest;

import java.util.List;
import java.util.UUID;

public interface OrderApi {

    @GetMapping("/api/v1/order")
    List<OrderDto> getClientOrders(@RequestParam @NotBlank(message = "Имя пользователя не может быть пустым") String username);

    @PutMapping("/api/v1/order")
    OrderDto createNewOrder(@RequestBody CreateNewOrderRequest request);

    @PostMapping("/api/v1/order/return")
    OrderDto productReturn(@RequestBody ProductReturnRequest request);

    @PostMapping("/api/v1/order/payment")
    OrderDto payment(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/payment/failed")
    OrderDto paymentFailed(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/payment/success")
    OrderDto paymentSuccess(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/delivery")
    OrderDto delivery(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/delivery/failed")
    OrderDto deliveryFailed(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/completed")
    OrderDto complete(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/calculate/total")
    OrderDto calculateTotalCost(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/calculate/delivery")
    OrderDto calculateDeliveryCost(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/assembly")
    OrderDto assembly(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/assembly/failed")
    OrderDto assemblyFailed(@RequestBody UUID orderId);
}