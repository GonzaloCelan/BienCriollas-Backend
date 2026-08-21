package com.bienCriollas.stock.estadistica.dto;

import java.util.List;

public record AnalisisIaResponse(
        String titulo,
        String analisis,
        List<String> recomendaciones
) {}
