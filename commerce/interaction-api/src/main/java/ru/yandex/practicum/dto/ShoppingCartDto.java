package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.validation.CheckCart;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCartDto {

    @NotNull(groups = {CheckCart.class}, message = "ID корзины не может быть пустым")
    private UUID shoppingCartId;

    @NotEmpty(groups = {CheckCart.class}, message = "Список товаров не может быть пустым")
    private Map<UUID, @Positive Integer> products;
}