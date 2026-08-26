package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos completos necesarios para crear o reemplazar un pedido.")
public record PedidoRequestDTO (
		
	@Schema(description = "Nombre o referencia del cliente.", example = "Juan Pérez") String cliente,
	@Schema(description = "Canal de venta.", example = "PARTICULAR", allowableValues = {"PARTICULAR", "PEDIDOS_YA"}) String tipoVenta,
	@Schema(description = "Medio de pago.", example = "COMBINADO", allowableValues = {"EFECTIVO", "TRANSFERENCIA", "COMBINADO"}) String tipoPago,
	@Schema(description = "Número externo cuando la venta proviene de PedidosYa.", example = "PYA-483", nullable = true) String numeroPedidoPedidosYa,
	@Schema(description = "Horario prometido de entrega.", example = "21:30:00", nullable = true) LocalTime horaEntrega,
	@Schema(description = "Importe abonado en efectivo; se utiliza para pagos combinados.", example = "5000.00") BigDecimal montoEfectivo,
	@Schema(description = "Importe abonado por transferencia; se utiliza para pagos combinados.", example = "4000.00") BigDecimal montoTransferencia,
	@Schema(description = "Importe total del pedido.", example = "9000.00") BigDecimal totalPedido,
	@Schema(description = "Variedades y cantidades que reemplazan el detalle completo.") List<PedidoDetalleRequestDTO> detalles	
		
	) {}
