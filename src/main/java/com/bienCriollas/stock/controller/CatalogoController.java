package com.bienCriollas.stock.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.variedades.ActualizarPrecioDTO;
import com.bienCriollas.stock.Interface.IVariedadEmpanadaService;
import com.bienCriollas.stock.model.VariedadEmpanada;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final IVariedadEmpanadaService variedadEmpanadaService;

    @GetMapping
    public ResponseEntity<List<VariedadEmpanada>> obtenerCatalogo() {

        return ResponseEntity.ok(
                variedadEmpanadaService.obtenerCatalogo()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<VariedadEmpanada> actualizarPrecios(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPrecioDTO dto) {

        return ResponseEntity.ok(
                variedadEmpanadaService.actualizarPrecios(id, dto)
        );
    }
}