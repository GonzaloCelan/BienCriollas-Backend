package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record PedidoRequestDTO (
		
	String cliente,
	String tipoVenta,
	String tipoPago,
	String numeroPedidoPedidosYa,
	LocalTime horaEntrega,
	BigDecimal totalPedido,
	List<PedidoDetalleRequestDTO> detalles	
		
	) {}
