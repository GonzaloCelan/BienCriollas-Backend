package com.bienCriollas.stock.Interface;

import java.util.List;

import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Model.IngresoPedidosYa;

public interface IPedidosYaService {
	
	IngresoPedidosYa registrarLiquidacionPedidosYa(PedidosYaRequestDTO request);
	
	List<IngresoPedidosYa> obtenerLiquidacionesPorMes(int año, int mes);

     List<IngresoPedidosYa> obtenerTodasLasLiquidaciones();

}
