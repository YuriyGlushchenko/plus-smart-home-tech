package ru.yandex.practicum.cart.exception;

public class CartDeactivatedException extends RuntimeException {
    public CartDeactivatedException(String message) {
        super(message);
    }
}
