package com.bienCriollas.stock.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Dto.variedades.ActualizarPrecioDTO;
import com.bienCriollas.stock.Interface.IVariedadEmpanadaService;
import com.bienCriollas.stock.model.VariedadEmpanada;
import com.bienCriollas.stock.repository.VariedadEmpanadaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VariedadEmpanadaService implements IVariedadEmpanadaService {

    private final VariedadEmpanadaRepository variedadEmpanadaRepository;

    @Override
    public List<VariedadEmpanada> obtenerCatalogo() {
        return variedadEmpanadaRepository.findByActivo(1);
    }

    @Override
    @Transactional
    public VariedadEmpanada actualizarPrecios(
            Long id,
            ActualizarPrecioDTO dto) {

        VariedadEmpanada variedad = variedadEmpanadaRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No existe la variedad con id: " + id
                        )
                );

        variedad.setPrecioUnitario(dto.precioUnitario());
        variedad.setPrecioMediaDocena(dto.precioMediaDocena());
        variedad.setPrecioDocena(dto.precioDocena());

        return variedadEmpanadaRepository.save(variedad);
    }
}