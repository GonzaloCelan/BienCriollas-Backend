package com.bienCriollas.stock.Controller;

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
import org.springframework.web.bind.annotation.RestController;


import com.bienCriollas.stock.Dto.EgresoResponseDTO;
import com.bienCriollas.stock.Dto.EgresoTipoDTO;
import com.bienCriollas.stock.Dto.EgresosPorcentajeDTO;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.TipoEgreso;

import com.bienCriollas.stock.Service.EgresoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/egreso")
@RequiredArgsConstructor
public class EgresoController {

	
		private final EgresoService service;
		
		@PostMapping("/registrar")
	    public ResponseEntity<Egreso> registrarEgreso(@RequestBody EgresoTipoDTO request) {
	        Egreso response = service.registrarEgreso(request);
	        return ResponseEntity.status(HttpStatus.CREATED).body(response);
	    }
		
		@GetMapping("/acumulado")
	    public ResponseEntity<EgresoResponseDTO> obtenerEgresoAcumulado() {
			EgresoResponseDTO response = service.calcularEgresoAcumulado();
	        return ResponseEntity.status(HttpStatus.CREATED).body(response);
	    }
		
		@GetMapping("/diario")
	    public ResponseEntity<List<Egreso>> obtenerEgresoDiario() {
			
			List<Egreso> response = service.obtenerEgresosDeHoy();
			
	        return ResponseEntity.status(HttpStatus.CREATED).body(response);
	    }
		
		@GetMapping("/tipo/{tipo}")
		  public ResponseEntity<Page<Egreso>> listarPorTipo(
		      @PathVariable TipoEgreso tipo,
		      @PageableDefault(size = 10) Pageable pageable
		  ) {
		    return ResponseEntity.ok(service.listarPorTipoEgreso(tipo, pageable));
		  }
		
		// GET /api/egresos/kpis
	    @GetMapping("/porcentajes")
	    public ResponseEntity<List<EgresosPorcentajeDTO>> obtenerKpisEgresos() {
	        return ResponseEntity.ok(service.obtenerKpisMesActualVsAnterior());
	    }
}
