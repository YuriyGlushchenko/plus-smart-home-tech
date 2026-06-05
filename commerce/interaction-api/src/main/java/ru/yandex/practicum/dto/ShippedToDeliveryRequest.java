package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippedToDeliveryRequest {

    @NotNull(message = "ID заказа не может быть пустым")
    private UUID orderId;

    @NotNull(message = "ID доставки не может быть пустым")
    private UUID deliveryId;
}