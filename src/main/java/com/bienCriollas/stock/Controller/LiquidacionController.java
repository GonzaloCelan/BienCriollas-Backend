package com.bienCriollas.stock.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Model.LiquidacionSemanal;
import com.bienCriollas.stock.Service.LiquidacionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/liquidacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LiquidacionController {

    private final LiquidacionService liquidacionService;

    // ============================================================
    // GENERAR LIQUIDACIÓN SEMANAL
    // ============================================================
    @PostMapping("/generar")
    public LiquidacionSemanal generar(
            @RequestParam Long idEmpleado,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaFin
    ) {
        return liquidacionService.generarLiquidacion(idEmpleado, semanaInicio, semanaFin);
    }

    // ============================================================
    // MARCAR COMO PAGADA
    // ============================================================
    @PutMapping("/{idLiquidacion}/pagar")
    public void pagar(@PathVariable Long idLiquidacion) {
        liquidacionService.marcarComoPagada(idLiquidacion);
    }
    
    @PostMapping("/pagarSemana")
    public void pagarSemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicio
    ) {
        liquidacionService.pagarSemanaCompleta(semanaInicio);
    }


    // ============================================================
    // HISTORIAL DE UN EMPLEADO
    // ============================================================
    @GetMapping("/historial/{idEmpleado}")
    public List<LiquidacionSemanal> historial(@PathVariable Long idEmpleado) {
        return liquidacionService.historialEmpleado(idEmpleado);
    }
    
    @GetMapping("/semana")
    public List<LiquidacionSemanal> buscar(
            @RequestParam String inicio,
            @RequestParam String fin
    ) {
        LocalDate f1 = LocalDate.parse(inicio.trim());
        LocalDate f2 = LocalDate.parse(fin.trim());

        return liquidacionService.buscarPorSemana(f1, f2);
    }



}