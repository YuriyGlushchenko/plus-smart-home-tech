package ru.yandex.practicum.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.model.ProductCategory;
import ru.yandex.practicum.model.ProductState;
import ru.yandex.practicum.model.QuantityState;
import ru.yandex.practicum.validation.Create;
import ru.yandex.practicum.validation.Update;


import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    @Null(groups = Create.class, message = "ID генерируется при сохранении")
    @NotNull(groups = Update.class, message = "ID обязателен для обновления")
    private UUID productId;

    @NotBlank(message = "Наименование товара не может быть пустым")
    @Size(max = 255, message = "Наименование не должно превышать 255 символов")
    private String productName;

    @NotBlank(message = "Описание товара не может быть пустым")
    private String description;

    private String imageSrc;

    @NotNull(message = "Статус количества должен быть указан")
    private QuantityState quantityState;

    @NotNull(message = "Статус товара должен быть указан")
    private ProductState productState;

    private ProductCategory productCategory;

    @NotNull(message = "Цена товара должна быть указана")
    @DecimalMin(value = "1.0", message = "Цена должна быть не меньше 1")
    @Digits(integer = 10, fraction = 2, message = "Цена должна иметь не более 2 десятичных знаков")
    private BigDecimal price;
}