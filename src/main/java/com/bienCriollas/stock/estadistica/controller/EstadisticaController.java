package com.bienCriollas.stock.estadistica.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.estadistica.dto.EstadisticaResumenDTO;
import com.bienCriollas.stock.estadistica.service.EstadisticaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/estadisticas")
@RequiredArgsConstructor
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    @GetMapping("/resumen")
    public ResponseEntity<EstadisticaResumenDTO> obtenerResumen(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta
    ) {
        return ResponseEntity.ok(
                estadisticaService.obtenerResumen(desde, hasta)
        );
    }
}