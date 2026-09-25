package com.bienCriollas.stock.employee.dto;

public record EmployeeDashboardResponseDTO(
        Long activeEmployees,
        EmployeeDashboardMetricDTO today,
        EmployeeDashboardMetricDTO week,
        EmployeeDashboardMetricDTO month
) {}
