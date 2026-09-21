-- Los ingredientes existentes conservan su costo unitario actual. No se inventa una
-- presentación comercial porque no puede deducirse de los datos históricos.
ALTER TABLE ingredients
    ADD COLUMN purchase_presentation VARCHAR(100) NULL AFTER measurement_unit,
    ADD COLUMN purchase_quantity DECIMAL(19,4) NULL AFTER purchase_presentation,
    ADD COLUMN purchase_price DECIMAL(19,2) NULL AFTER purchase_quantity,
    ADD CONSTRAINT ck_ingredients_purchase_data_complete CHECK (
        (purchase_presentation IS NULL
            AND purchase_quantity IS NULL
            AND purchase_price IS NULL)
        OR
        (purchase_presentation IS NOT NULL
            AND purchase_quantity IS NOT NULL
            AND purchase_price IS NOT NULL)
    ),
    ADD CONSTRAINT ck_ingredients_purchase_presentation CHECK (
        purchase_presentation IS NULL OR CHAR_LENGTH(TRIM(purchase_presentation)) > 0
    ),
    ADD CONSTRAINT ck_ingredients_purchase_quantity CHECK (
        purchase_quantity IS NULL OR purchase_quantity > 0
    ),
    ADD CONSTRAINT ck_ingredients_purchase_price CHECK (
        purchase_price IS NULL OR purchase_price > 0
    );
