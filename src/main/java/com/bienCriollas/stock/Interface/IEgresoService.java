package com.bienCriollas.stock.Interface;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.Dto.EgresoResponseDTO;
import com.bienCriollas.stock.Dto.EgresoTipoDTO;
import com.bienCriollas.stock.Dto.EgresosPorcentajeDTO;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.TipoEgreso;

public interface IEgresoService {

	public Egreso registrarEgreso(EgresoTipoDTO request);
	
	public EgresoResponseDTO calcularEgresoAcumulado();
	
	public List<Egreso> obtenerEgresosDeHoy();
	
	public Page<Egreso> listarPorTipoEgreso(TipoEgreso tipo, Pageable pageable);
	
	public List<EgresosPorcentajeDTO> obtenerKpisMesActualVsAnterior();	
}
