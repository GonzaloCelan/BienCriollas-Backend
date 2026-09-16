package com.bienCriollas.stock.production.process.exception;

public class InvalidProcessReferenceYieldException extends RuntimeException {
    public InvalidProcessReferenceYieldException() {
        super("El rendimiento de referencia debe ser mayor a 0.");
    }
}
