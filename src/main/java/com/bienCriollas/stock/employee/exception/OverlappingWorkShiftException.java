package com.bienCriollas.stock.employee.exception;

public class OverlappingWorkShiftException extends InvalidWorkDayException {
    public OverlappingWorkShiftException() {
        super("Los turnos de la jornada no pueden superponerse.");
    }
}
