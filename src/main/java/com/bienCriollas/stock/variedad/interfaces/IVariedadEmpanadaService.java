package com.bienCriollas.stock.variedad.interfaces;


import java.util.List;

import com.bienCriollas.stock.variedad.dto.ActualizarPrecioDTO;
import com.bienCriollas.stock.variedad.entity.VariedadEmpanada;

public interface IVariedadEmpanadaService {

    List<VariedadEmpanada> obtenerCatalogo();

    VariedadEmpanada actualizarPrecios(
            Long id,
            ActualizarPrecioDTO dto
    );
}
