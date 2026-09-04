package com.bienCriollas.stock.expense.exception;

public class InvalidExpenseException extends IllegalArgumentException {

    public InvalidExpenseException(String message) {
        super(message);
    }

    public InvalidExpenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
