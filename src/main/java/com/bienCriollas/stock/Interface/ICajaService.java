package com.bienCriollas.stock.Interface;

import java.time.LocalDate;
import java.util.List;

import com.bienCriollas.stock.Dto.BalanceResponseDTO;
import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.IngresoPedidosYa;

public interface ICajaService {

	
	 CajaDiaria registrarCierreDeCaja(LocalDate fecha);
	 
	 
	 CajaDiaria obtenerCajaPorFecha(LocalDate fecha);

	    
	 List<CajaDiaria> obtenerCajasPorMes(int año, int mes);

	    
	 List<CajaDiaria> obtenerTodasLasCajas();
	 
	 CajaResponseDTO previsualizarCaja(LocalDate fecha);
	 

}
