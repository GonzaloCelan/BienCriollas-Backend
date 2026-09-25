package com.bienCriollas.stock.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.employee.entity.EmployeeWorkShift;

public interface EmployeeWorkShiftRepository extends JpaRepository<EmployeeWorkShift, Long> {
}
