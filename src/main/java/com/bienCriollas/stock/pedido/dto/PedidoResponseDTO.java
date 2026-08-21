package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.bienCriollas.stock.pedido.enums.TipoEstado;

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
