-- Los valores históricos fueron creados con CURRENT_TIMESTAMP en la zona
-- configurada en cada servidor. Se normalizan una única vez a UTC.
UPDATE liquidacion_pedidos_ya
SET creado_en = TIMESTAMPADD(
    SECOND,
    -TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(), CURRENT_TIMESTAMP()),
    creado_en
);

-- A partir de esta versión el backend siempre proporciona creado_en en UTC.
ALTER TABLE liquidacion_pedidos_ya
    MODIFY creado_en DATETIME NOT NULL;
