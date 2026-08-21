package com.bienCriollas.stock.estadistica.dto;

import java.math.BigDecimal;
import java.util.List;

import com.bienCriollas.stock.merma.dto.EmpanadaMermaDTO;

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
