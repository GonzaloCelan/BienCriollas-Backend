package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos completos necesarios para crear o reemplazar un pedido.")
public record OrderRequestDTO (

    @JsonProperty("cliente")
    @Schema(description = "Nombre o referencia del cliente.", example = "Juan Pérez") String customer,
    @JsonProperty("tipoVenta")
    @Schema(description = "Canal de venta.", example = "PARTICULAR", allowableValues = {"PARTICULAR", "PEDIDOS_YA"}) String saleType,
    @JsonProperty("tipoPago")
    @Schema(description = "Medio de pago.", example = "COMBINADO", allowableValues = {"EFECTIVO", "TRANSFERENCIA", "COMBINADO"}) String paymentType,
    @JsonProperty("numeroPedidoPedidosYa")
    @Schema(description = "Número externo cuando la venta proviene de PedidosYa.", example = "PYA-483", nullable = true) String pedidosYaOrderNumber,
    @JsonProperty("horaEntrega")
    @Schema(description = "Horario prometido de entrega.", example = "21:30:00", nullable = true) LocalTime deliveryTime,
    @JsonProperty("montoEfectivo")
    @Schema(description = "Importe abonado en efectivo; se utiliza para pagos combinados.", example = "5000.00") BigDecimal cashAmount,
    @JsonProperty("montoTransferencia")
    @Schema(description = "Importe abonado por transferencia; se utiliza para pagos combinados.", example = "4000.00") BigDecimal transferAmount,
    @JsonProperty("totalPedido")
    @Schema(description = "Importe total del pedido.", example = "9000.00") BigDecimal orderTotal,
    @JsonProperty("detalles")
    @Schema(description = "Variedades y cantidades que reemplazan el detalle completo.") List<OrderDetailRequestDTO> details

    ) {}
