package com.bienCriollas.stock.variety.interfaces;


import java.util.List;

import com.bienCriollas.stock.variety.dto.UpdatePriceDTO;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

public interface IEmpanadaVarietyService {

    List<EmpanadaVariety> getCatalog();

    EmpanadaVariety updatePrices(
            Long varietyId,
            UpdatePriceDTO request
    );
}
