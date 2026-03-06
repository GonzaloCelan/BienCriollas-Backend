package com.bienCriollas.stock.Interface;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.bienCriollas.stock.Dto.IngresosDiariosDTO;
import com.bienCriollas.stock.Dto.PedidoDetalleResponseDTO;
import com.bienCriollas.stock.Dto.PedidoRequestDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.enums.TipoEstado;
import com.bienCriollas.stock.enums.TipoPago;

public interface IPedidoService {
	
	  public PedidoResponseDTO crearPedido(PedidoRequestDTO pedidoDTO);
	  
	  public boolean actualizarEstadoPedido(Long idPedido, TipoEstado nuevoEstado);
	  
	  public boolean actualizarTipoPago(Long idPedido, TipoPago nuevoTipoPago);
	  
	  public IngresosDiariosDTO calcularIngresosDiarios(LocalDate fecha , TipoEstado estado);
	  
	  public List<PedidoResponseDTO> obtenerTodosLosPedidos(TipoEstado estado);
	  
	  public List<PedidoResponseDTO> obtenerPedidosPorFecha(LocalDate fechaInicio);
	  
	  public List<PedidoDetalleResponseDTO> obtenerDetallesPedido(Long idPedido);
	  
	  public Page<PedidoResponseDTO> obtenerPedidosPaginados(TipoEstado estado, int page, int size);

	  public Page<PedidoResponseDTO> obtenerPedidosPaginadosPorEstadoYFecha(TipoEstado estado, LocalDate fecha, int page,
			int size);
	}
