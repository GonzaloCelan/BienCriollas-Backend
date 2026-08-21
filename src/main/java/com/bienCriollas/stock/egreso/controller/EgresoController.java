package com.bienCriollas.stock.egreso.controller;

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

import com.bienCriollas.stock.egreso.dto.EgresoResponseDTO;
import com.bienCriollas.stock.egreso.dto.EgresoTipoDTO;
import com.bienCriollas.stock.egreso.dto.EgresoTotalPorTipoDTO;
import com.bienCriollas.stock.egreso.dto.EgresosPorcentajeDTO;
import com.bienCriollas.stock.egreso.interfaces.IEgresoService;
import com.bienCriollas.stock.egreso.enums.TipoEgreso;
import com.bienCriollas.stock.egreso.entity.Egreso;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v2/egreso")
@RequiredArgsConstructor
public class EgresoController {

    private final IEgresoService service;

    /*
     * ==========================
     * CREAR EGRESO
     * ==========================
     */

    @PostMapping("/registrar")
    public ResponseEntity<Egreso> registrarEgreso(@RequestBody EgresoTipoDTO request) {
        Egreso response = service.registrarEgreso(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * ==========================
     * KPIS / RESÚMENES
     * ==========================
     */

    @GetMapping("/acumulado")
    public ResponseEntity<EgresoResponseDTO> obtenerEgresoAcumulado() {
        EgresoResponseDTO response = service.calcularEgresoAcumulado();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/porcentajes")
    public ResponseEntity<List<EgresosPorcentajeDTO>> obtenerKpisEgresos() {
        List<EgresosPorcentajeDTO> response = service.obtenerKpisMesActualVsAnterior();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/totales-tipo")
    public ResponseEntity<List<EgresoTotalPorTipoDTO>> obtenerTotalesPorTipo(
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        List<EgresoTotalPorTipoDTO> response = service.obtenerTotalesPorTipo(anio, mes);
        return ResponseEntity.ok(response);
    }

    /*
     * ==========================
     * LISTADOS
     * ==========================
     */

    @GetMapping("/diario")
    public ResponseEntity<List<Egreso>> obtenerEgresoDiario() {
        List<Egreso> response = service.obtenerEgresosDeHoy();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<Page<Egreso>> listarPorTipo(
            @PathVariable TipoEgreso tipo,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<Egreso> response = service.listarPorTipoEgreso(tipo, pageable);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/historial")
    public ResponseEntity<Page<Egreso>> listarHistorial(
            @RequestParam int anio,
            @RequestParam int mes,
            @RequestParam(required = false) TipoEgreso tipo,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<Egreso> response;

        if (tipo != null) {
            response = service.listarHistorial(anio, mes, tipo, pageable);
        } else {
            response = service.listarHistorial(anio, mes, pageable);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ultimos")
    public ResponseEntity<List<Egreso>> obtenerUltimosMovimientos() {
        List<Egreso> response = service.obtenerUltimosMovimientos();
        return ResponseEntity.ok(response);
    }
}