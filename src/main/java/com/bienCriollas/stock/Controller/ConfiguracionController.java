package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.PrecioCostoVariedadDTO;
import com.bienCriollas.stock.Dto.VariedadEmpanadaDTO;
import com.bienCriollas.stock.Model.VariedadEmpanada;
import com.bienCriollas.stock.Service.ConfiguracionService;
import com.bienCriollas.stock.Service.EmpleadoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/variedad")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor

public class ConfiguracionController {

	
	private final ConfiguracionService service;
	
	
    @PutMapping("/precio-costo")
    public ResponseEntity<List<VariedadEmpanadaDTO>> actualizarPreciosCostos(
            @RequestBody List<PrecioCostoVariedadDTO> requestList
    ) {
        List<VariedadEmpanadaDTO> actualizadas = service.actualizarPrecioCostoVariedad(requestList);
        return ResponseEntity.ok(actualizadas);
    }
    
    @PostMapping("/agregar")
    public ResponseEntity<Boolean> agregarVariedadNueva(
            @RequestBody VariedadEmpanada request
    ) {
        Boolean response = service.añadirVariedadNueva(request);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/eliminar")
    public ResponseEntity<VariedadEmpanada> eliminarVariedad(
			@RequestParam Long idVariedad
	) {
    	VariedadEmpanada response = service.eliminarVariedad(idVariedad);
		
		return ResponseEntity.ok(response);
	}
	
}
