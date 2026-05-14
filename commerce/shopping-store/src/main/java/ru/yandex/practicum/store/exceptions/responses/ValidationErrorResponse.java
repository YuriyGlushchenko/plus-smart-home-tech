package ru.yandex.practicum.store.exceptions.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorResponse {
    private String code;
    private String message;
    private Map<String, String> errors;
}