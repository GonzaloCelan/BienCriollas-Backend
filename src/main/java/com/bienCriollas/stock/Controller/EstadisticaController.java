package com.bienCriollas.stock.Controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.EstadisticaDTO;
import com.bienCriollas.stock.Model.TipoEstado;
import com.bienCriollas.stock.Service.EstadisticaService;
import com.bienCriollas.stock.Service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/estadistica")
@RequiredArgsConstructor
public class EstadisticaController {

	private final EstadisticaService estadisticaService;
		
	@GetMapping("/{fecha}")
	public ResponseEntity<EstadisticaDTO> obtenerEstadisticaPorFecha(@PathVariable 
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDate fecha) {
	    
		EstadisticaDTO response = estadisticaService.obtenerEstadisticasPorFecha(fecha);
		
		 return ResponseEntity.ok(response);
}
	
	@GetMapping("/mes/{año}/{mes}")
	public EstadisticaDTO obtenerEstadisticaPorMes(
	        @PathVariable int año,
	        @PathVariable int mes) {

	    return estadisticaService.obtenerEstadisticasPorMes(año, mes);
	}
}