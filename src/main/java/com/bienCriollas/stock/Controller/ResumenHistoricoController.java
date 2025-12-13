package com.bienCriollas.stock.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.ResumenAcumuladoDTO;
import com.bienCriollas.stock.Service.PedidoService;
import com.bienCriollas.stock.Service.ResumenHistorialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resumen")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ResumenHistoricoController {

	private final ResumenHistorialService resumenService;
	
	
	@GetMapping("/acumulado")
	public ResponseEntity<ResumenAcumuladoDTO> obtenerAcumulado() {
		
	    ResumenAcumuladoDTO dto = resumenService.obtenerAcumuladoHistorico();
	    
	    return (dto != null) ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
	}

}
