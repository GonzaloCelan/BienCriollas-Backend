package com.bienCriollas.stock.employee.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.enums.EmployeeStatus;
import com.bienCriollas.stock.employee.interfaces.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/employees")
@RequiredArgsConstructor
@Tag(name = "Empleados", description = "Empleados, valor hora e historial de jornadas.")
public class EmployeeController {

    private final IEmployeeService employeeService;
    private final IEmployeeWorkDayService workDayService;

    @PostMapping
    @Operation(summary = "Crear empleado")
    public ResponseEntity<EmployeeResponseDTO> create(
            @Valid @RequestBody EmployeeRequestDTO request) {
        EmployeeResponseDTO response = employeeService.create(request);
        return ResponseEntity.created(URI.create("/api/v2/employees/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Listar empleados por estado")
    public ResponseEntity<List<EmployeeResponseDTO>> list(
            @RequestParam(defaultValue = "ACTIVE") EmployeeStatus status) {
        return ResponseEntity.ok(employeeService.list(status));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar empleados activos por nombre")
    public ResponseEntity<List<EmployeeResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(employeeService.search(query));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Obtener indicadores del módulo de empleados")
    public ResponseEntity<EmployeeDashboardResponseDTO> dashboard() {
        return ResponseEntity.ok(workDayService.dashboard());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener empleado")
    public ResponseEntity<EmployeeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar empleado sin alterar snapshots históricos")
    public ResponseEntity<EmployeeResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequestDTO request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar empleado")
    public ResponseEntity<EmployeeResponseDTO> activate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar empleado conservando su historial")
    public ResponseEntity<EmployeeResponseDTO> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivate(id));
    }

    @GetMapping("/{id}/workdays")
    @Operation(summary = "Obtener historial paginado y totales de un empleado")
    public ResponseEntity<EmployeeWorkDayHistoryResponseDTO> workDays(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"workDate", "id"},
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(workDayService.employeeHistory(id, from, to, pageable));
    }
}
