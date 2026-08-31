CREATE TABLE IF NOT EXISTS regularizacion_pedido (
    id_regularizacion BIGINT NOT NULL AUTO_INCREMENT,
    id_lote VARCHAR(36) NOT NULL,
    id_pedido INT UNSIGNED NOT NULL,
    estado_anterior VARCHAR(20) NOT NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    realizado_por VARCHAR(80) NOT NULL,
    motivo VARCHAR(250) NOT NULL,
    realizado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_regularizacion_pedido PRIMARY KEY (id_regularizacion),
    CONSTRAINT fk_regularizacion_pedido_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT chk_regularizacion_estado_anterior
        CHECK (estado_anterior IN ('PENDIENTE', 'PREPARADO')),
    CONSTRAINT chk_regularizacion_estado_nuevo
        CHECK (estado_nuevo = 'ENTREGADO')
);

CREATE INDEX idx_regularizacion_pedido_lote
    ON regularizacion_pedido (id_lote);

CREATE INDEX idx_regularizacion_pedido_fecha
    ON regularizacion_pedido (realizado_en);

CREATE INDEX idx_regularizacion_pedido_pedido
    ON regularizacion_pedido (id_pedido);
