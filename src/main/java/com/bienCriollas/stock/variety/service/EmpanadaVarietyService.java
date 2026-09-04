package com.bienCriollas.stock.variety.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bienCriollas.stock.variety.dto.UpdatePriceDTO;
import com.bienCriollas.stock.variety.interfaces.IEmpanadaVarietyService;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpanadaVarietyService implements IEmpanadaVarietyService {

    private final EmpanadaVarietyRepository empanadaVarietyRepository;

    @Override
    public List<EmpanadaVariety> getCatalog() {
        return empanadaVarietyRepository.findByActive(1);
    }

    @Override
    @Transactional
    public EmpanadaVariety updatePrices(
            Long varietyId,
            UpdatePriceDTO request) {

        EmpanadaVariety variety = empanadaVarietyRepository
                .findById(varietyId)
                .orElseThrow(() -> new VarietyNotFoundException(varietyId));

        variety.setUnitPrice(request.unitPrice());
        variety.setHalfDozenPrice(request.halfDozenPrice());
        variety.setDozenPrice(request.dozenPrice());

        return empanadaVarietyRepository.save(variety);
    }
}
