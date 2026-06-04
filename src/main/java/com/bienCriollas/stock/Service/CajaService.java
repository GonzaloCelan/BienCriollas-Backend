package com.bienCriollas.stock.Service;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.EgresosDiariosDTO;

import com.bienCriollas.stock.Dto.IngresosDiariosDTO;

import com.bienCriollas.stock.Interface.ICajaService;
import com.bienCriollas.stock.Model.CajaDiaria;



import com.bienCriollas.stock.Repository.CajaDiariaRepository;


import com.bienCriollas.stock.enums.EstadoCaja;
import com.bienCriollas.stock.enums.TipoEstado;


import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CajaService implements ICajaService {


	
    private final CajaDiariaRepository cajaDiariaRepository;
    
    private final EgresoService egresoService;
    private final PerdidasService perdidasService;
    private final PedidoService pedidoService;
	@Override
	public CajaDiaria registrarCierreDeCaja(LocalDate fecha) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public CajaDiaria obtenerCajaPorFecha(LocalDate fecha) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<CajaDiaria> obtenerCajasPorMes(int año, int mes) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<CajaDiaria> obtenerTodasLasCajas() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public CajaResponseDTO previsualizarCaja(LocalDate fecha) {
		// TODO Auto-generated method stub
		return null;
	}
    
    
  
   
    
  


    
}
