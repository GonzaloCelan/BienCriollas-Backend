package com.bienCriollas.stock.waste.interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IWasteService {


    BigDecimal calculateWasteByDate(LocalDate date);

    List<Object[]> getWasteValueByVarietyAndMonth(int year, int month);

    List<Object[]> getAllWasteWithValue();
}
