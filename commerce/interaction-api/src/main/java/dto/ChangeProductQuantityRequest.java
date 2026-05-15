package dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeProductQuantityRequest {

    @NotNull(message = "ID товара не может быть пустым")
    private UUID productId;

    @NotNull(message = "Новое количество не может быть пустым")
    @PositiveOrZero(message = "Количество не может быть отрицательным")
    private Long newQuantity; // в спецификации int64, т.е. Long. Но логичнее Integer?
}