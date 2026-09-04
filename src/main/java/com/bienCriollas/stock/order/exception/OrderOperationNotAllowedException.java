package com.bienCriollas.stock.order.exception;

public class OrderOperationNotAllowedException extends IllegalStateException {

    public OrderOperationNotAllowedException(String message) {
        super(message);
    }
}
