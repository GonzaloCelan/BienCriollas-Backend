package com.bienCriollas.stock.employee.exception;

public class WorkDayNotFoundException extends RuntimeException {
    public WorkDayNotFoundException(Long id) {
        super("No se encontró la jornada con id " + id + ".");
    }
}
