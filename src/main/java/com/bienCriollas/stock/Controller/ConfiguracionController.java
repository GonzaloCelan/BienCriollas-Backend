package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.PrecioCostoVariedadDTO;
import com.bienCriollas.stock.Dto.VariedadEmpanadaDTO;
import com.bienCriollas.stock.Service.ConfiguracionService;
import com.bienCriollas.stock.Service.EmpleadoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/configuracion")
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
	
}
