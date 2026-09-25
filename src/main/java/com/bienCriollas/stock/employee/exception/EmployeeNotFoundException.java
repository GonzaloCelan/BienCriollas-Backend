package com.bienCriollas.stock.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super("No se encontró el empleado con id " + id + ".");
    }
}
