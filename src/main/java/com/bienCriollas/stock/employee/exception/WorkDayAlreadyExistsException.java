package com.bienCriollas.stock.employee.exception;

import java.time.LocalDate;

public class WorkDayAlreadyExistsException extends RuntimeException {
    public WorkDayAlreadyExistsException(String employeeName, LocalDate date) {
        super("El empleado '" + employeeName
                + "' ya posee una jornada registrada para la fecha " + date + ".");
    }
}
