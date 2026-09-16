ALTER TABLE pedido
    ADD COLUMN fecha_entrega DATE NULL,
    ADD COLUMN stock_discounted BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_pedido_fecha_entrega
    ON pedido(fecha_entrega);

CREATE INDEX idx_pedido_fecha_entrega_estado
    ON pedido(fecha_entrega, estado);
