package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.pedido.enums.TipoPago;
import com.bienCriollas.stock.pedido.enums.TipoVenta;

public record PedidoDetalleResponseDTO(
		
		String cliente,
		Long idVariedad, 
		String nombreVariedad, // nombre de la variedad de empanada     // id de la variedad de empanada
        Integer cantidad,
        BigDecimal subtotal,
        TipoVenta tipoVenta,
        TipoPago tipoPago
        ) {

}
