package ru.yandex.practicum.exceptions.exceptions;

public class NoProductsInShoppingCartException extends RuntimeException {

    public NoProductsInShoppingCartException(String message) {
        super(message);
    }

    public NoProductsInShoppingCartException(String message, Throwable cause) {
        super(message, cause);
    }
}