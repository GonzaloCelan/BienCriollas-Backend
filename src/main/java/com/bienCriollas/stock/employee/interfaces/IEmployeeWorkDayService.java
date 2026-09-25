package com.bienCriollas.stock.employee.interfaces;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.enums.WorkDayPeriod;

public interface IEmployeeWorkDayService {
    EmployeeWorkDayResponseDTO create(EmployeeWorkDayCreateRequestDTO request);
    List<EmployeeWorkDayResponseDTO> createBulk(EmployeeWorkDayBulkCreateRequestDTO request);
    EmployeeWorkDayResponseDTO getById(Long id);
    EmployeeWorkDayResponseDTO update(Long id, EmployeeWorkDayUpdateRequestDTO request);
    void delete(Long id);
    EmployeeWorkDayResponseDTO copy(Long id, EmployeeWorkDayCopyRequestDTO request);
    Page<EmployeeWorkDayResponseDTO> history(Long employeeId, LocalDate date,
            LocalDate from, LocalDate to, WorkDayPeriod period, Pageable pageable);
    EmployeeWorkDayHistoryResponseDTO employeeHistory(Long employeeId,
            LocalDate from, LocalDate to, Pageable pageable);
    EmployeeWeekResponseDTO week(LocalDate date);
    EmployeePeriodSummaryResponseDTO weekSummary(LocalDate date);
    EmployeeMonthSummaryResponseDTO monthSummary(Integer year, Integer month);
    EmployeeDaySummaryResponseDTO daySummary(LocalDate date);
    EmployeeDashboardResponseDTO dashboard();
}
