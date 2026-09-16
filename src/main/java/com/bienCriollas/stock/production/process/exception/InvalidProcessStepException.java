package com.bienCriollas.stock.production.process.exception;

public class InvalidProcessStepException extends RuntimeException {
    public InvalidProcessStepException(String message) {
        super(message);
    }

    public static InvalidProcessStepException activeWithoutPeople(String stepName) {
        return new InvalidProcessStepException(
                "El paso '" + stepName + "' es de trabajo activo y requiere al menos una persona.");
    }
}
