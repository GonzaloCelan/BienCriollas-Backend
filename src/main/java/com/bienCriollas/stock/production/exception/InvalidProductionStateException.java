package com.bienCriollas.stock.production.exception;

public class InvalidProductionStateException extends RuntimeException {
    public InvalidProductionStateException(String message) {
        super(message);
    }
}
