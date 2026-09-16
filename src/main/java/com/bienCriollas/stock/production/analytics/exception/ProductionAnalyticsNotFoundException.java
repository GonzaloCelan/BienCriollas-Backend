package com.bienCriollas.stock.production.analytics.exception;

public class ProductionAnalyticsNotFoundException extends RuntimeException {

    public ProductionAnalyticsNotFoundException(Long productionId) {
        super("No existe una producción finalizada con id " + productionId + ".");
    }

    public ProductionAnalyticsNotFoundException(String message) {
        super(message);
    }
}
