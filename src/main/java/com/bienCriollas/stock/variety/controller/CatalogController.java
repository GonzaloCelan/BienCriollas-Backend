package com.bienCriollas.stock.variety.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.variety.dto.UpdatePriceDTO;
import com.bienCriollas.stock.variety.interfaces.IEmpanadaVarietyService;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Consulta de variedades y administración de precios.")
public class CatalogController {

    private final IEmpanadaVarietyService empanadaVarietyService;

    @GetMapping
    @Operation(summary = "Obtener el catálogo", description = "Lista todas las variedades con sus precios vigentes.")
    public ResponseEntity<List<EmpanadaVariety>> getCatalog() {

        return ResponseEntity.ok(
                empanadaVarietyService.getCatalog()
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar precios", description = "Modifica los precios unitario, media docena y docena de una variedad.")
    public ResponseEntity<EmpanadaVariety> updatePrices(
            @PathVariable("id") Long varietyId,
            @Valid @RequestBody UpdatePriceDTO request) {

        return ResponseEntity.ok(
                empanadaVarietyService.updatePrices(varietyId, request)
        );
    }
}
