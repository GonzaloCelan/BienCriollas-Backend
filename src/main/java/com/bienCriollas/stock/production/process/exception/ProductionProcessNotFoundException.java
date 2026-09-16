package com.bienCriollas.stock.production.process.exception;

public class ProductionProcessNotFoundException extends RuntimeException {
    public ProductionProcessNotFoundException(Long id) {
        super("Proceso con id " + id + " no encontrado.");
    }

    public static ProductionProcessNotFoundException forVariety(Long varietyId) {
        return new ProductionProcessNotFoundException(
                "No existe un proceso vigente para la variedad con id " + varietyId + ".");
    }

    private ProductionProcessNotFoundException(String message) {
        super(message);
    }
}
