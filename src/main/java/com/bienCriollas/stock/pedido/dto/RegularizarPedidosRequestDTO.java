package com.bienCriollas.stock.pedido.dto;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegularizarPedidosRequestDTO(
        @NotNull @Min(2000) @Max(2100) Integer anio,
        @NotNull @Min(1) @Max(12) Integer mes,
        @NotEmpty @Size(max = 200) List<@NotNull Long> idsPedidos,
        @NotBlank @Size(max = 250) String motivo,
        @NotNull @AssertTrue(message = "Debés confirmar expresamente la regularización") Boolean confirmar) {
}
