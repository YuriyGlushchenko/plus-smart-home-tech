package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class OrderDto {

    @NotNull(message = "ID заказа не может быть пустым")
    private UUID orderId;

    @NotNull(message = "ID корзины не может быть пустым")
    private UUID shoppingCartId;

    @NotEmpty(message = "Список товаров не может быть пустым")
    private Map<UUID, Integer> products;

    private UUID paymentId;

    private UUID deliveryId;

    private String state;

    @PositiveOrZero(message = "Вес доставки не может быть отрицательным")
    private Double deliveryWeight;

    @PositiveOrZero(message = "Объём доставки не может быть отрицательным")
    private Double deliveryVolume;

    private Boolean fragile;

    @PositiveOrZero(message = "Общая стоимость не может быть отрицательной")
    private Double totalPrice;

    @PositiveOrZero(message = "Стоимость доставки не может быть отрицательной")
    private Double deliveryPrice;

    @PositiveOrZero(message = "Стоимость товаров не может быть отрицательной")
    private Double productPrice;
}