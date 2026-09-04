package com.bienCriollas.stock.expense.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.expense.dto.ExpenseResponseDTO;
import com.bienCriollas.stock.expense.dto.ExpenseTypeDTO;
import com.bienCriollas.stock.expense.dto.ExpenseTotalByTypeDTO;
import com.bienCriollas.stock.expense.dto.ExpensePercentageDTO;
import com.bienCriollas.stock.expense.interfaces.IExpenseService;
import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.bienCriollas.stock.expense.entity.Expense;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v2/egreso")
@RequiredArgsConstructor
@Tag(name = "Egresos", description = "Registro, consultas e indicadores de gastos.")
public class ExpenseController {

    private final IExpenseService expenseService;

    /*
     * ==========================
     * CREAR EGRESO
     * ==========================
     */

    @PostMapping("/registrar")
    @Operation(summary = "Registrar un egreso", description = "Guarda un nuevo gasto categorizado.")
    public ResponseEntity<Expense> registerExpense(@RequestBody ExpenseTypeDTO request) {
        Expense response = expenseService.registerExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * ==========================
     * KPIS / RESÚMENES
     * ==========================
     */

    @GetMapping("/acumulado")
    @Operation(summary = "Obtener egreso acumulado", description = "Calcula el total acumulado del período utilizado por el servicio.")
    public ResponseEntity<ExpenseResponseDTO> getAccumulatedExpenses() {
        ExpenseResponseDTO response = expenseService.calculateAccumulatedExpenses();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/porcentajes")
    @Operation(summary = "Comparar egresos mensuales", description = "Devuelve indicadores del mes actual frente al anterior.")
    public ResponseEntity<List<ExpensePercentageDTO>> getExpenseMetrics() {
        List<ExpensePercentageDTO> response = expenseService.getCurrentVsPreviousMonthMetrics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/totales-tipo")
    @Operation(summary = "Obtener totales por tipo", description = "Agrupa los egresos por categoría para el mes indicado.")
    public ResponseEntity<List<ExpenseTotalByTypeDTO>> getTotalsByType(
            @RequestParam("anio") int year,
            @RequestParam("mes") int month
    ) {
        List<ExpenseTotalByTypeDTO> response = expenseService.getTotalsByType(year, month);
        return ResponseEntity.ok(response);
    }

    /*
     * ==========================
     * LISTADOS
     * ==========================
     */

    @GetMapping("/diario")
    @Operation(summary = "Obtener egresos de hoy", description = "Lista los gastos registrados durante el día actual.")
    public ResponseEntity<List<Expense>> getDailyExpenses() {
        List<Expense> response = expenseService.getTodaysExpenses();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Listar egresos por tipo", description = "Devuelve un historial paginado filtrado por categoría.")
    public ResponseEntity<Page<Expense>> getByType(
            @PathVariable("tipo") ExpenseType expenseType,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<Expense> response = expenseService.getByExpenseType(expenseType, pageable);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/historial")
    @Operation(summary = "Consultar historial mensual", description = "Lista egresos por año y mes, con filtro opcional por tipo.")
    public ResponseEntity<Page<Expense>> getHistory(
            @RequestParam("anio") int year,
            @RequestParam("mes") int month,
            @RequestParam(value = "tipo", required = false) ExpenseType expenseType,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<Expense> response;

        if (expenseType != null) {
            response = expenseService.getHistory(year, month, expenseType, pageable);
        } else {
            response = expenseService.getHistory(year, month, pageable);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ultimos")
    @Operation(summary = "Obtener últimos egresos", description = "Devuelve los movimientos de egreso más recientes.")
    public ResponseEntity<List<Expense>> getLatestMovements() {
        List<Expense> response = expenseService.getLatestMovements();
        return ResponseEntity.ok(response);
    }
}
