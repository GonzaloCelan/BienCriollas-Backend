package com.bienCriollas.stock.expense.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para registrar un egreso.")
public record ExpenseTypeDTO(

        @JsonProperty("idCaja") @Schema(description = "ID de caja, cuando corresponda.", example = "1", nullable = true) Long cashRegisterId,
        @JsonProperty("tipoEgreso") @Schema(description = "Categoría del gasto.", example = "PRODUCCION") ExpenseType expenseType,
        @JsonProperty("descripcion") @Schema(description = "Detalle del gasto.", example = "Compra de harina") String description,
        @JsonProperty("monto") @Schema(description = "Importe del egreso.", example = "25000.00") BigDecimal amount) {

}
