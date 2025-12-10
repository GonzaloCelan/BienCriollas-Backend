package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.bienCriollas.stock.Model.TipoEstado;

public record PedidoResponseDTO(
		Long idPedido,
		String cliente,
		String tipoVenta,
		String tipoPago,
		String numeroPedidoPedidosYa,
		LocalTime horaEntrega,
		BigDecimal totalPedido,
		TipoEstado estadoPedido
		
		) 

{}
