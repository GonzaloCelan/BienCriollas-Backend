package com.bienCriollas.stock.stock.interfaces;

import java.util.List;

import com.bienCriollas.stock.waste.dto.EmpanadaLossDTO;
import com.bienCriollas.stock.stock.dto.StockAdjustmentDTO;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.dto.StockResponseDTO;

public interface IStockService {

    Boolean updateStock(List<StockDTO> requests);
    
    Boolean decreaseVarietyStock(Long varietyId, Integer quantityToDecrease);
    
    List<StockResponseDTO> getAllStockRecords();
    
    List<StockResponseDTO> getStockRecordsByVariety(Long varietyId);
    
    void registerLosses(List<EmpanadaLossDTO> losses);

    void adjustAvailableStock(List<StockAdjustmentDTO> adjustments);
}
