package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderDetailResponseDTO(
		@JsonProperty("cliente") String customer,
		@JsonProperty("idVariedad") Long varietyId,
		@JsonProperty("nombreVariedad") String varietyName,
        @JsonProperty("cantidad") Integer quantity,
        BigDecimal subtotal,
        @JsonProperty("tipoVenta") SaleType saleType,
        @JsonProperty("tipoPago") PaymentType paymentType
        ) {

}
