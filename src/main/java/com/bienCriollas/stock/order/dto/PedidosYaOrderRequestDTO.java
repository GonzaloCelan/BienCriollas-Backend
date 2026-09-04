package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PedidosYaOrderRequestDTO(

        @JsonProperty("fecha") LocalDate date,
        @JsonProperty("monto") BigDecimal amount) {

}
