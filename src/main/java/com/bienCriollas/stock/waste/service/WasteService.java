package com.bienCriollas.stock.waste.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.waste.exception.InvalidWasteException;
import com.bienCriollas.stock.waste.interfaces.IWasteService;
import com.bienCriollas.stock.waste.repository.WasteRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class WasteService implements IWasteService {

    private final WasteRepository wasteRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateWasteByDate(LocalDate date) {
        if (date == null) {
            throw new InvalidWasteException("La fecha es obligatoria");
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return wasteRepository.sumAmountByDate(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getWasteValueByVarietyAndMonth(int year, int month) {
        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            throw new InvalidWasteException("El año o el mes no son válidos");
        }

                return wasteRepository.getWasteValueByVarietyAndMonth(year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getAllWasteWithValue() {
        return wasteRepository.getAllWasteWithValue();
    }
}
