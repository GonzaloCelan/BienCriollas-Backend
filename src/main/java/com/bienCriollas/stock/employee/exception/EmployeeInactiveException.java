package com.bienCriollas.stock.employee.exception;

public class EmployeeInactiveException extends RuntimeException {
    public EmployeeInactiveException(String name) {
        super("El empleado '" + name + "' está inactivo y no puede recibir nuevas jornadas.");
    }
}
