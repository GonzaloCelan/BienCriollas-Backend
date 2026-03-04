package com.bienCriollas.stock.Interface;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.bienCriollas.stock.Dto.PedidoDetalleResponseDTO;
import com.bienCriollas.stock.Dto.PedidoRequestDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.enums.TipoEstado;
import com.bienCriollas.stock.enums.TipoPago;

public interface IPedidoService {
	
	  PedidoResponseDTO crearPedido(PedidoRequestDTO pedidoDTO);
	  
	  boolean actualizarEstadoPedido(Long idPedido, TipoEstado nuevoEstado);
	  
	  boolean actualizarTipoPago(Long idPedido, TipoPago nuevoTipoPago);
	  
	  List<PedidoResponseDTO> obtenerTodosLosPedidos(TipoEstado estado);
	  
	  List<PedidoResponseDTO> obtenerPedidosPorFecha(LocalDate fechaInicio);
	  
	  List<PedidoDetalleResponseDTO> obtenerDetallesPedido(Long idPedido);
	  
	  public Page<PedidoResponseDTO> obtenerPedidosPaginados(TipoEstado estado, int page, int size);

	Page<PedidoResponseDTO> obtenerPedidosPaginadosPorEstadoYFecha(TipoEstado estado, LocalDate fecha, int page,
			int size);
	}
