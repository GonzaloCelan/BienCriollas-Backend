package com.bienCriollas.stock.employee.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EmployeeBulkConflictResponseDTO(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<EmployeeConflictDTO> conflicts
) {}
