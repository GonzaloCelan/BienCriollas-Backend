package com.bienCriollas.stock.expense.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExpenseResponseDTO(

        @JsonProperty("totalPersonal") BigDecimal personnelTotal,
        @JsonProperty("totalProduccion") BigDecimal productionTotal,
        @JsonProperty("totalOtros") BigDecimal otherTotal

        ) {

}
