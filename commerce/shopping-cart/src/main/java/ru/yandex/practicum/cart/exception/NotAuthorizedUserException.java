package ru.yandex.practicum.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class NotAuthorizedUserException extends RuntimeException {

    public NotAuthorizedUserException(String message) {
        super(message);
    }

    public NotAuthorizedUserException(String message, Throwable cause) {
        super(message, cause);
    }
}