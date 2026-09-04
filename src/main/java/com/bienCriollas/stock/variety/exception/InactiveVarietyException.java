package com.bienCriollas.stock.variety.exception;

public class InactiveVarietyException extends IllegalStateException {

    public InactiveVarietyException(Long varietyId) {
        super("La variedad con id " + varietyId + " no está activa");
    }
}
