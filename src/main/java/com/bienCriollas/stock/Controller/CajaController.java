package com.bienCriollas.stock.Controller;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.BalanceResponseDTO;
import com.bienCriollas.stock.Dto.CajaEstadoDTO;
import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.EgresoRequestDTO;
import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.CajaEgreso;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Service.CajaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @PostMapping("/registrar")
    public ResponseEntity<CajaEgreso> registrarEgresos(@RequestBody EgresoRequestDTO dto) {
        return ResponseEntity.ok(cajaService.registrarEgreso(dto));
    }
    
    @PostMapping("/registrar-py")
    public ResponseEntity<IngresoPedidosYa> registraPedidosYa(@RequestBody PedidosYaRequestDTO dto) {
        return ResponseEntity.ok(cajaService.registrarIngresoPY(dto));
    }
    
    
    // Endpoint para obtener los egreos del dia actual o de un dia en especifico
    @GetMapping("/egresos")
    public ResponseEntity<List<CajaEgreso>> listaDeEgresos(
            @RequestParam(required = false) LocalDate fecha
    ) {
        // si no viene fecha → usar hoy
        LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now();

        return ResponseEntity.ok(
                cajaService.obtenerEgresosDelDia(fechaConsulta)
        );
    }
    
    
    @GetMapping("/ingresos")
    public ResponseEntity<CajaResponseDTO> obtenerIngresos(
            @RequestParam LocalDate fecha
        
    ) {

        CajaResponseDTO resp = cajaService.registrarIngresos(fecha);
        return ResponseEntity.ok(resp);
    }
    
    
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponseDTO> obtenerBalance(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

    	BalanceResponseDTO response = cajaService.calcularBalanceDiario(fecha);
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping("/cierre")
    public ResponseEntity<CajaDiaria> registarCierreDeCaja(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

    	CajaDiaria response = cajaService.registrarCierreDeCaja(fecha);
        return ResponseEntity.ok(response);
    }

}