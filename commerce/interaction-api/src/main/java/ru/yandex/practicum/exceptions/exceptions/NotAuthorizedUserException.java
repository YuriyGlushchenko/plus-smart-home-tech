package ru.yandex.practicum.exceptions.exceptions;

public class NotAuthorizedUserException extends RuntimeException {

    public NotAuthorizedUserException(String message) {
        super(message);
    }

    public NotAuthorizedUserException(String message, Throwable cause) {
        super(message, cause);
    }
}