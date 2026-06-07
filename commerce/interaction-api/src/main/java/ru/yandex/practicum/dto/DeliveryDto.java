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
public class DeliveryDto {

    private UUID deliveryId;

    @NotNull(message = "Адрес отправителя не может быть пустым")
    @Valid
    private AddressDto fromAddress;

    @NotNull(message = "Адрес получателя не может быть пустым")
    @Valid
    private AddressDto toAddress;

    @NotNull(message = "ID заказа не может быть пустым")
    private UUID orderId;

    private String deliveryState;

    @Positive(message = "Вес доставки должен быть положительным числом")
    private Double deliveryWeight;

    @Positive(message = "Объём доставки должен быть положительным числом")
    private Double deliveryVolume;

    private Boolean fragile;
}