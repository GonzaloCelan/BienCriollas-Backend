package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.Model.TipoVenta;

public record PedidoDetalleResponseDTO(
		
		String cliente,
		Long idVariedad, 
		String nombreVariedad, // nombre de la variedad de empanada     // id de la variedad de empanada
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        TipoVenta tipoVenta
        ) {

}
