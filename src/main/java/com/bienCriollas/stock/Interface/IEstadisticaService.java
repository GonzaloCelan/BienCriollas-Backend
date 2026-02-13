package com.bienCriollas.stock.Interface;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.Dto.EstadisticaDTO;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.TipoVenta;

public interface IEstadisticaService {

	
	public EstadisticaDTO obtenerEstadisticasPorFecha(LocalDate fecha);
	
	public EstadisticaDTO obtenerEstadisticasPorMes(int año, int mes);
	
	
	public EstadisticaDTO obtenerEstadisticasUltimos7Dias();
	
	public Page<Pedido> listarEntregadosDelDia(LocalDate fecha,TipoVenta tipoVenta, Pageable pageable);
	
	public Page<Pedido> listarEntregadosDelMes(int anio, int mes,TipoVenta tipoVenta, Pageable pageable);
}
