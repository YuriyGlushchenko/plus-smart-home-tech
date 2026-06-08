package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyProductsForOrderRequest {

    @NotNull(message = "ID заказа не может быть пустым")
    private UUID orderId;

    @NotNull(message = "Список товаров не может быть пустым")
    private Map<UUID, Integer> products;
}