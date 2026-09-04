package com.bienCriollas.stock.security.exception;

public class InvalidUserException extends IllegalArgumentException {

    public InvalidUserException(String message) {
        super(message);
    }
}
