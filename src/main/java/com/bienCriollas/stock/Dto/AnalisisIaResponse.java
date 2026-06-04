package com.bienCriollas.stock.Dto;

import java.util.List;

public record AnalisisIaResponse(
        String titulo,
        String analisis,
        List<String> recomendaciones
) {}
