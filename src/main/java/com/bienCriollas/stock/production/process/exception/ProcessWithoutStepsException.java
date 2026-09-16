package com.bienCriollas.stock.production.process.exception;

public class ProcessWithoutStepsException extends RuntimeException {
    public ProcessWithoutStepsException() {
        super("El proceso debe contener al menos un paso.");
    }
}
