package com.bienCriollas.stock.employee.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.enums.WorkDayPeriod;
import com.bienCriollas.stock.employee.interfaces.IEmployeeWorkDayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/employee-workdays")
@RequiredArgsConstructor
@Tag(name = "Jornadas de empleados",
        description = "Turnos, horas trabajadas, importes e historial operativo.")
public class EmployeeWorkDayController {

    private final IEmployeeWorkDayService workDayService;

    @PostMapping
    @Operation(summary = "Crear jornada")
    public ResponseEntity<EmployeeWorkDayResponseDTO> create(
            @Valid @RequestBody EmployeeWorkDayCreateRequestDTO request) {
        EmployeeWorkDayResponseDTO response = workDayService.create(request);
        return ResponseEntity.created(
                URI.create("/api/v2/employee-workdays/" + response.id())).body(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Crear la misma jornada para varios empleados")
    public ResponseEntity<List<EmployeeWorkDayResponseDTO>> createBulk(
            @Valid @RequestBody EmployeeWorkDayBulkCreateRequestDTO request) {
        return ResponseEntity.status(201).body(workDayService.createBulk(request));
    }

    @GetMapping("/history")
    @Operation(summary = "Consultar historial general paginado")
    public ResponseEntity<Page<EmployeeWorkDayResponseDTO>> history(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) WorkDayPeriod period,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"workDate", "id"},
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(workDayService.history(
                employeeId, date, from, to, period, pageable));
    }

    @GetMapping("/week")
    @Operation(summary = "Obtener planilla de la semana que contiene la fecha")
    public ResponseEntity<EmployeeWeekResponseDTO> week(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(workDayService.week(date));
    }

    @GetMapping("/summary/week")
    @Operation(summary = "Obtener resumen semanal por empleado")
    public ResponseEntity<EmployeePeriodSummaryResponseDTO> weekSummary(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(workDayService.weekSummary(date));
    }

    @GetMapping("/summary/month")
    @Operation(summary = "Obtener resumen mensual por empleado")
    public ResponseEntity<EmployeeMonthSummaryResponseDTO> monthSummary(
            @RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(workDayService.monthSummary(year, month));
    }

    @GetMapping("/summary/day")
    @Operation(summary = "Obtener horas-persona y costo de personal del día")
    public ResponseEntity<EmployeeDaySummaryResponseDTO> daySummary(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(workDayService.daySummary(date));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener jornada con todos sus turnos")
    public ResponseEntity<EmployeeWorkDayResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workDayService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar jornada conservando el valor hora histórico")
    public ResponseEntity<EmployeeWorkDayResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeWorkDayUpdateRequestDTO request) {
        return ResponseEntity.ok(workDayService.update(id, request));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "Copiar jornada usando el valor hora actual del empleado")
    public ResponseEntity<EmployeeWorkDayResponseDTO> copy(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeWorkDayCopyRequestDTO request) {
        EmployeeWorkDayResponseDTO response = workDayService.copy(id, request);
        return ResponseEntity.created(
                URI.create("/api/v2/employee-workdays/" + response.id())).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una jornada cargada por error")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workDayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
