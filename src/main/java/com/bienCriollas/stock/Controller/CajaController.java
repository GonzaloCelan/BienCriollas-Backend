package com.bienCriollas.stock.Controller;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.BalanceResponseDTO;
import com.bienCriollas.stock.Dto.CajaEstadoDTO;
import com.bienCriollas.stock.Dto.CajaMetaResponseDTO;
import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.EgresoRequestDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Interface.ICajaService;
import com.bienCriollas.stock.Interface.IPedidoService;
import com.bienCriollas.stock.Interface.IPedidosYaService;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Service.CajaService;
import com.bienCriollas.stock.Service.PedidoService;
import com.bienCriollas.stock.enums.TipoEstado;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

	
	
	private final ICajaService cajaService;
	
	
    private final  IPedidoService pedidoService;
	
	
 
    @GetMapping("/preview/{fecha}")
    public ResponseEntity<CajaResponseDTO> previewCaja(@PathVariable LocalDate fecha) {
        return ResponseEntity.ok(cajaService.previsualizarCaja(fecha));
    }
    
    
    @PostMapping("/cierre")
    public ResponseEntity<CajaDiaria> registarCierreDeCaja(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

    	CajaDiaria response = cajaService.registrarCierreDeCaja(fecha);
        return ResponseEntity.ok(response);
    }
    
   
    @GetMapping("/pedidos")
 
    public ResponseEntity<Page<PedidoResponseDTO>> obtenerPedidosPorEstadoYFecha(
            @RequestParam TipoEstado estado,

            // opcional: si no viene, toma "hoy" (Argentina)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        if (fecha == null) {
            fecha = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        }

        Page<PedidoResponseDTO> result =
                pedidoService.obtenerPedidosPaginadosPorEstadoYFecha(estado, fecha, page, size);

        return result.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(result);
    }

}