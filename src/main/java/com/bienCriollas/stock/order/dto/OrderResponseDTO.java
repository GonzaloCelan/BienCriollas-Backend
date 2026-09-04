package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.bienCriollas.stock.order.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderResponseDTO(
        @JsonProperty("idPedido") Long orderId,
        @JsonProperty("cliente") String customer,
        @JsonProperty("tipoVenta") String saleType,
        @JsonProperty("tipoPago") String paymentType,
        @JsonProperty("numeroPedidoPedidosYa") String pedidosYaOrderNumber,
        @JsonProperty("horaEntrega") LocalTime deliveryTime,
        @JsonProperty("totalPedido") BigDecimal orderTotal,
        @JsonProperty("estadoPedido") OrderStatus orderStatus

        )

{}
