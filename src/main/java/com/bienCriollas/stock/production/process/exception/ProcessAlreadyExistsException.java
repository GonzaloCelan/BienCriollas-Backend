package com.bienCriollas.stock.production.process.exception;

public class ProcessAlreadyExistsException extends RuntimeException {
    public ProcessAlreadyExistsException(String varietyName) {
        super("La variedad " + varietyName + " ya tiene un proceso vigente.");
    }
}
