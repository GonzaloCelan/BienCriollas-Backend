package com.bienCriollas.stock.ingreso.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.ingreso.dto.IngresoResumenDTO;
import com.bienCriollas.stock.ingreso.dto.LiquidacionPedidosYaRequestDTO;
import com.bienCriollas.stock.ingreso.service.IngresoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/ingresos")
@RequiredArgsConstructor
@Tag(name = "Ingresos", description = "Resúmenes de cobros y liquidaciones de canales externos.")
public class IngresoController {

    private final IngresoService ingresoService;

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen de ingresos", description = "Consolida los ingresos del período indicado.")
    public ResponseEntity<IngresoResumenDTO> obtenerResumen(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta
    ) {
        return ResponseEntity.ok(
                ingresoService.obtenerResumen(desde, hasta)
        );
    }

    @PostMapping("/liquidaciones-pedidos-ya")
    @Operation(summary = "Registrar liquidación de PedidosYa", description = "Registra el importe liquidado por la plataforma para una fecha.")
    public ResponseEntity<Void> registrarLiquidacionPedidosYa(
            @RequestBody LiquidacionPedidosYaRequestDTO request
    ) {
        ingresoService.registrarLiquidacionPedidosYa(request);
        return ResponseEntity.ok().build();
    }
}
