package com.bienCriollas.stock.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("No se encontró el pedido con id " + orderId);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
