package com.bienCriollas.stock.stock.interfaces;

import java.util.List;

import com.bienCriollas.stock.merma.dto.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.stock.dto.AjusteStockDTO;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.dto.StockResponseDTO;

public interface IStockService {

    public Boolean actualizarStock(List<StockDTO> requestList);
    
    public Boolean descontarStockVariedad(Long idVariedad, Integer cantidadADescontar);
    
    public List<StockResponseDTO> obtenerTodosLosRegistrosDeStock();
    
    public List<StockResponseDTO> obtenerRegistrosDeStockPorVariedad(Long idVariedad);
    
    public void registrarPerdidas(List<PerdidaEmpanadaDTO> perdidas);

    public void ajustarStockDisponible(List<AjusteStockDTO> ajustes);
}