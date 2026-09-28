package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Schema(description = "Variedades y cantidades que reemplazan el detalle completo.") List<OrderDetailRequestDTO> details,
    @JsonProperty("fechaEntrega")
    @Schema(description = "Fecha solicitada de entrega. Si es futura, el pedido queda programado sin descontar stock.", example = "2026-09-13", nullable = true) LocalDate deliveryDate,
    @JsonProperty("pagado")
    @Schema(description = "Indica si el cobro ya fue confirmado. En creación, si se omite, es false.", example = "false", nullable = true) Boolean pagado,
    @JsonProperty("tiempoEstimadoDeliveryMinutos")
    @Schema(description = "Minutos informados por PedidosYa hasta la llegada del repartidor. Es opcional y solo aplica a PEDIDOS_YA.", example = "31", nullable = true) Integer estimatedDeliveryMinutes

    ) {
    public OrderRequestDTO(
            String customer,
            String saleType,
            String paymentType,
            String pedidosYaOrderNumber,
            LocalTime deliveryTime,
            BigDecimal cashAmount,
            BigDecimal transferAmount,
            BigDecimal orderTotal,
            List<OrderDetailRequestDTO> details) {
        this(customer, saleType, paymentType, pedidosYaOrderNumber, deliveryTime,
                cashAmount, transferAmount, orderTotal, details, null, null, null);
    }

    public OrderRequestDTO(
            String customer,
            String saleType,
            String paymentType,
            String pedidosYaOrderNumber,
            LocalTime deliveryTime,
            BigDecimal cashAmount,
            BigDecimal transferAmount,
            BigDecimal orderTotal,
            List<OrderDetailRequestDTO> details,
            LocalDate deliveryDate) {
        this(customer, saleType, paymentType, pedidosYaOrderNumber, deliveryTime,
                cashAmount, transferAmount, orderTotal, details, deliveryDate, null, null);
    }

    public OrderRequestDTO(
            String customer,
            String saleType,
            String paymentType,
            String pedidosYaOrderNumber,
            LocalTime deliveryTime,
            BigDecimal cashAmount,
            BigDecimal transferAmount,
            BigDecimal orderTotal,
            List<OrderDetailRequestDTO> details,
            LocalDate deliveryDate,
            Boolean pagado) {
        this(customer, saleType, paymentType, pedidosYaOrderNumber, deliveryTime,
                cashAmount, transferAmount, orderTotal, details, deliveryDate, pagado, null);
    }
}
