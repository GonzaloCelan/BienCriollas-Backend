package com.bienCriollas.stock.Service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Dto.ResumenAcumuladoDTO;
import com.bienCriollas.stock.Interface.CajaAcumuladoProjection;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.MermaRepository;
import com.bienCriollas.stock.Repository.StockRepository;
import com.bienCriollas.stock.Repository.VariedadEmpanadaRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ResumenHistorialService {

	private final CajaDiariaRepository cajaDiariaRepository;
	
	
	public ResumenAcumuladoDTO obtenerAcumuladoHistorico() {
		
        CajaAcumuladoProjection p = cajaDiariaRepository.obtenerAcumuladoHistoricoCajasCerradas();

        // por las dudas, evitamos nulls
        BigDecimal ef  = (p == null || p.getAcumuladoEfectivo() == null) ? BigDecimal.ZERO : p.getAcumuladoEfectivo();
        BigDecimal tr  = (p == null || p.getAcumuladoTransferencia() == null) ? BigDecimal.ZERO : p.getAcumuladoTransferencia();
        BigDecimal py  = (p == null || p.getAcumuladoPedidosya() == null) ? BigDecimal.ZERO : p.getAcumuladoPedidosya();
        BigDecimal tot = (p == null || p.getAcumuladoTotal() == null) ? BigDecimal.ZERO : p.getAcumuladoTotal();
        BigDecimal eg  = (p == null || p.getEgresoAcumulado() == null) ? BigDecimal.ZERO : p.getEgresoAcumulado();

        return new ResumenAcumuladoDTO(ef, tr, py, tot, eg);
    }
	
}
