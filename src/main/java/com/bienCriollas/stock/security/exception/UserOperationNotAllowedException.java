package com.bienCriollas.stock.security.exception;

public class UserOperationNotAllowedException extends IllegalStateException {

    public UserOperationNotAllowedException(String message) {
        super(message);
    }
}
