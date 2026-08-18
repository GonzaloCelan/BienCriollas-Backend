package com.bienCriollas.stock.Interface;


import java.util.List;

import com.bienCriollas.stock.Dto.variedades.ActualizarPrecioDTO;
import com.bienCriollas.stock.model.VariedadEmpanada;

public interface IVariedadEmpanadaService {

    List<VariedadEmpanada> obtenerCatalogo();

    VariedadEmpanada actualizarPrecios(
            Long id,
            ActualizarPrecioDTO dto
    );
}
