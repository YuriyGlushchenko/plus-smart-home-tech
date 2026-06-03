package ru.yandex.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionDto {

    @NotNull(message = "Ширина обязательна")
    @Min(value = 1, message = "Ширина должна быть не меньше 1")
    private Double width;

    @NotNull(message = "Высота обязательна")
    @Min(value = 1, message = "Высота должна быть не меньше 1")
    private Double height;

    @NotNull(message = "Глубина обязательна")
    @Min(value = 1, message = "Глубина должна быть не меньше 1")
    private Double depth;
}