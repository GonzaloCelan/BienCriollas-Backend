package com.bienCriollas.stock.Dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CajaMetaResponseDTO(
		boolean existe,
        LocalDate fecha,
        String estado,          
        LocalDateTime cerradaEn ) {

}
