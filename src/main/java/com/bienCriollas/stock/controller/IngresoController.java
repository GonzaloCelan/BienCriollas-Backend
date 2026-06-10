package com.bienCriollas.stock.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.ingreso.IngresoResumenDTO;
import com.bienCriollas.stock.Dto.ingreso.LiquidacionPedidosYaRequestDTO;
import com.bienCriollas.stock.service.IngresoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/ingresos")
@RequiredArgsConstructor
public class IngresoController {

    private final IngresoService ingresoService;

    @GetMapping("/resumen")
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
    public ResponseEntity<Void> registrarLiquidacionPedidosYa(
            @RequestBody LiquidacionPedidosYaRequestDTO request
    ) {
        ingresoService.registrarLiquidacionPedidosYa(request);
        return ResponseEntity.ok().build();
    }
}