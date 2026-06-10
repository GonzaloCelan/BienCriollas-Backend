package com.bienCriollas.stock.Interface;

import java.util.List;

import com.bienCriollas.stock.Dto.mermas.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.Dto.stocks.AjusteStockDTO;
import com.bienCriollas.stock.Dto.stocks.StockDTO;
import com.bienCriollas.stock.Dto.stocks.StockResponseDTO;

public interface IStockService {

    public Boolean actualizarStock(List<StockDTO> requestList);
    
    public Boolean descontarStockVariedad(Long idVariedad, Integer cantidadADescontar);
    
    public List<StockResponseDTO> obtenerTodosLosRegistrosDeStock();
    
    public List<StockResponseDTO> obtenerRegistrosDeStockPorVariedad(Long idVariedad);
    
    public void registrarPerdidas(List<PerdidaEmpanadaDTO> perdidas);

    public void ajustarStockDisponible(List<AjusteStockDTO> ajustes);
}