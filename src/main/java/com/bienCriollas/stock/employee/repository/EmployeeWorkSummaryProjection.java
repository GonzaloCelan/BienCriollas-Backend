package com.bienCriollas.stock.employee.repository;

import java.math.BigDecimal;

public interface EmployeeWorkSummaryProjection {
    Long getEmployeeId();
    String getEmployeeName();
    Long getWorkedMinutes();
    BigDecimal getAmount();
}
