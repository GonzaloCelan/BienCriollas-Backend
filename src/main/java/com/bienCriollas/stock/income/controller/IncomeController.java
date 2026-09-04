package com.bienCriollas.stock.income.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.income.dto.IncomeSummaryDTO;
import com.bienCriollas.stock.income.dto.PedidosYaSettlementRequestDTO;
import com.bienCriollas.stock.income.service.IncomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/ingresos")
@RequiredArgsConstructor
@Tag(name = "Ingresos", description = "Resúmenes de cobros y liquidaciones de canales externos.")
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen de ingresos", description = "Consolida los ingresos del período indicado.")
    public ResponseEntity<IncomeSummaryDTO> getSummary(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end
    ) {
        return ResponseEntity.ok(
                incomeService.getSummary(start, end)
        );
    }

    @PostMapping("/liquidaciones-pedidos-ya")
    @Operation(summary = "Registrar liquidación de PedidosYa", description = "Registra el importe liquidado por la plataforma para una fecha.")
    public ResponseEntity<Void> registerPedidosYaSettlement(
            @RequestBody PedidosYaSettlementRequestDTO request
    ) {
        incomeService.registerPedidosYaSettlement(request);
        return ResponseEntity.ok().build();
    }
}
