package com.bienCriollas.stock.employee.exception;

public class InvalidWorkDayException extends RuntimeException {
    public InvalidWorkDayException(String message) {
        super(message);
    }
}
