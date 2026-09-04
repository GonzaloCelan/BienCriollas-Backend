package com.bienCriollas.stock.stock.exception;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long varietyId) {
        super("No hay stock registrado para la variedad con id " + varietyId);
    }
}
