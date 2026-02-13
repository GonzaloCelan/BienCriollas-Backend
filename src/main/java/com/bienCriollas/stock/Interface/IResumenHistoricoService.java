package com.bienCriollas.stock.Interface;

import java.util.List;

import com.bienCriollas.stock.Dto.BalanceMensualDTO;
import com.bienCriollas.stock.Dto.ResumenAcumuladoDTO;
import com.bienCriollas.stock.Model.IngresoPedidosYa;

public interface IResumenHistoricoService {

	public ResumenAcumuladoDTO obtenerAcumuladoHistorico(Integer anio, Integer mes);
	
	
	public List<BalanceMensualDTO> resumenMensualGrafico(Integer anio);
	
	
	public List<IngresoPedidosYa> obtenerPedidosYaLiquidaciones();
	
}
