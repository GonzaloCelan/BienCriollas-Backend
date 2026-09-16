-- V5 puede estar aplicada: se conserva intacta y se convierte el precio existente.
-- Una columna nueva evita perder los 6 decimales antes de multiplicar por 1000.
ALTER TABLE ingredients
    ADD COLUMN cost_per_kilogram DECIMAL(14,3) NULL AFTER cost_per_gram;

UPDATE ingredients
SET cost_per_kilogram = cost_per_gram * 1000;

ALTER TABLE ingredients
    DROP CHECK ck_ingredients_cost,
    DROP COLUMN cost_per_gram,
    MODIFY COLUMN cost_per_kilogram DECIMAL(14,3) NOT NULL,
    ADD CONSTRAINT ck_ingredients_cost_per_kilogram CHECK (cost_per_kilogram > 0);
