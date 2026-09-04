package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderReconciliationItemDTO(
        @JsonProperty("idPedido") Long orderId,
        @JsonProperty("fechaPedido") LocalDate orderDate,
        @JsonProperty("cliente") String customer,
        @JsonProperty("tipoVenta") SaleType saleType,
        @JsonProperty("tipoPago") PaymentType paymentType,
        @JsonProperty("numeroPedidoPedidosYa") String pedidosYaOrderNumber,
        @JsonProperty("horaEntrega") LocalTime deliveryTime,
        @JsonProperty("totalPedido") BigDecimal orderTotal,
        @JsonProperty("estadoActual") OrderStatus currentStatus) {
}
