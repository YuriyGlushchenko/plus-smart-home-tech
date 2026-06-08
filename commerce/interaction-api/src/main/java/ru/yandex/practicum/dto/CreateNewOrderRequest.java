package ru.yandex.practicum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewOrderRequest {

    @NotNull(message = "Корзина не может быть пустой")
    @Valid
    private ShoppingCartDto shoppingCart;

    @NotNull(message = "Адрес доставки обязателен")
    @Valid
    private AddressDto deliveryAddress;
}