package ru.yandex.practicum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewProductInWarehouseRequest {

    @NotNull(message = "ID товара не может быть пустым")
    private UUID productId;

    private Boolean fragile;

    @NotNull(message = "Размеры товара обязательны")
    @Valid
    private DimensionDto dimension;

    @NotNull(message = "Вес товара обязателен")
    @Positive(message = "Вес должен быть положительным")
    private Double weight;
}