package com.bienCriollas.stock.employee.exception;

import java.util.List;

import com.bienCriollas.stock.employee.dto.EmployeeConflictDTO;

public class BulkWorkDayConflictException extends RuntimeException {

    private final List<EmployeeConflictDTO> conflicts;

    public BulkWorkDayConflictException(List<EmployeeConflictDTO> conflicts) {
        super("Existen empleados con jornada ya registrada para esa fecha.");
        this.conflicts = List.copyOf(conflicts);
    }

    public List<EmployeeConflictDTO> getConflicts() {
        return conflicts;
    }
}
