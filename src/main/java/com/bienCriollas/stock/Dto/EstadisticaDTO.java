package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.util.List;

public record EstadisticaDTO(
		
		Integer totalEmpanadasVendidas,
		Integer totalMermas,
		BigDecimal totalMermasImporte,
		Integer totalPedidos,
		BigDecimal totalIngresos,
		BigDecimal totalEfectivo,
		BigDecimal totalTransferencia,
		BigDecimal totalPedidosYa,
		Integer variedadBajoStock,
		Integer cantidadPedidosPY,
		Integer cantidadParticular,
		List<EmpanadaVendidaDTO> empanadasMasVendidas,
		List<EmpanadaMermaDTO> empanadasPerdidas
		
		) {

}
