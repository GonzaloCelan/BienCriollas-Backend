package com.bienCriollas.stock.Interface;

import java.time.LocalDate;
import java.util.List;

import com.bienCriollas.stock.Dto.BalanceResponseDTO;
import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.IngresoPedidosYa;

public interface ICajaService {

	
	public CajaResponseDTO registrarIngresos(LocalDate fecha);
	
	public BalanceResponseDTO calcularBalanceDiario(LocalDate fecha);
	
	 public CajaDiaria registrarCierreDeCaja(LocalDate fecha);
	 
	 
	 public IngresoPedidosYa registrarLiquidacionPedidosYa(PedidosYaRequestDTO request);
}
