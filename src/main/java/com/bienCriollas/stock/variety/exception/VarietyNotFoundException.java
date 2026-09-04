package com.bienCriollas.stock.variety.exception;

public class VarietyNotFoundException extends RuntimeException {

    public VarietyNotFoundException(Long varietyId) {
        super("No se encontró la variedad con id " + varietyId);
    }
}
