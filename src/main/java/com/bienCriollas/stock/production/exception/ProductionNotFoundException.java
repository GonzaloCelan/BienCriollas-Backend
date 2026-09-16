package com.bienCriollas.stock.production.exception;

public class ProductionNotFoundException extends RuntimeException {
    public ProductionNotFoundException(Long id) {
        super("Producción con id " + id + " no encontrada.");
    }
}
