ALTER TABLE pedido
    ADD COLUMN created_at DATETIME NULL;

CREATE INDEX idx_pedido_created_at
    ON pedido(created_at);
