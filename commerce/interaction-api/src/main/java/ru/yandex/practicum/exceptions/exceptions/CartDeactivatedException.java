package ru.yandex.practicum.exceptions.exceptions;

public class CartDeactivatedException extends RuntimeException {
    public CartDeactivatedException(String message) {
        super(message);
    }
}
