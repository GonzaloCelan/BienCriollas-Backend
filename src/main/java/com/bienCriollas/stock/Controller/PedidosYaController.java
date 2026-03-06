package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Interface.IPedidosYaService;
import com.bienCriollas.stock.Model.IngresoPedidosYa;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidosya")
@RequiredArgsConstructor
public class PedidosYaController {

    private final IPedidosYaService pedidosYaService;

    @PostMapping("/liquidacion")
    public ResponseEntity<IngresoPedidosYa> registrarLiquidacion(@RequestBody PedidosYaRequestDTO request) {
        return ResponseEntity.ok(pedidosYaService.registrarLiquidacionPedidosYa(request));
    }

    @GetMapping("/liquidaciones")
    public ResponseEntity<List<IngresoPedidosYa>> obtenerTodas() {
        return ResponseEntity.ok(pedidosYaService.obtenerTodasLasLiquidaciones());
    }

    @GetMapping("/liquidaciones/mes")
    public ResponseEntity<List<IngresoPedidosYa>> obtenerPorMes(
            @RequestParam int año,
            @RequestParam int mes) {
        return ResponseEntity.ok(pedidosYaService.obtenerLiquidacionesPorMes(año, mes));
    }
}