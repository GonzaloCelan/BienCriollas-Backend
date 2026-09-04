package com.bienCriollas.stock.order.exception;

public class InvalidOrderException extends IllegalArgumentException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
