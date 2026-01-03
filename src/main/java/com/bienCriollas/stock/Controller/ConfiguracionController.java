package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.PrecioCostoVariedadDTO;
import com.bienCriollas.stock.Dto.VariedadEmpanadaDTO;
import com.bienCriollas.stock.Dto.VariedadRequestDTO;
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
    public ResponseEntity<VariedadEmpanada> agregarVariedadNueva(
            @RequestBody VariedadRequestDTO request
    ) {
        VariedadEmpanada response = service.añadirVariedadNueva(request);
        
        return ResponseEntity.ok(response);
    }
    
    
	
    @GetMapping("/obtener-activos")
    public ResponseEntity<List<VariedadEmpanada>> obtenerVariedadesActivas() {
		List<VariedadEmpanada> response = service.obtenerVariedadesActivas();
		return ResponseEntity.ok(response);
	}
    
    @PutMapping("/{idVariedad}/activo/{activo}")
    public ResponseEntity<VariedadEmpanada> setActivoVariedad(
            @PathVariable Long idVariedad,
            @PathVariable boolean activo
    ) {
        VariedadEmpanada response = service.setActivoVariedad(idVariedad, activo);
        return ResponseEntity.ok(response);
    }

    
}
