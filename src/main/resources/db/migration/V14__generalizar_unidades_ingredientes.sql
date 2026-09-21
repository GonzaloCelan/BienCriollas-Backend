-- Agrega primero el modelo genérico y copia los datos antes de retirar las columnas legacy.
-- Los registros existentes se conservan como GRAM porque no existe una conversión fiable
-- para reinterpretar automáticamente huevos o líquidos históricos.
ALTER TABLE ingredients
    ADD COLUMN measurement_unit VARCHAR(30) NULL AFTER name,
    ADD COLUMN current_stock DECIMAL(19,4) NULL AFTER measurement_unit,
    ADD COLUMN minimum_stock DECIMAL(19,4) NULL AFTER current_stock,
    ADD COLUMN cost_per_base_unit DECIMAL(19,6) NULL AFTER minimum_stock;

UPDATE ingredients
SET measurement_unit = 'GRAM',
    current_stock = current_stock_grams,
    minimum_stock = minimum_stock_grams,
    cost_per_base_unit = cost_per_kilogram / 1000;

ALTER TABLE recipe_ingredients
    ADD COLUMN quantity DECIMAL(19,4) NULL AFTER ingredient_id;

UPDATE recipe_ingredients
SET quantity = quantity_grams;

ALTER TABLE production_ingredients
    ADD COLUMN expected_quantity DECIMAL(19,4) NULL AFTER ingredient_id,
    ADD COLUMN actual_quantity DECIMAL(19,4) NULL AFTER expected_quantity,
    ADD COLUMN measurement_unit_snapshot VARCHAR(30) NULL AFTER actual_quantity,
    ADD COLUMN cost_per_base_unit_snapshot DECIMAL(19,6) NULL
        AFTER measurement_unit_snapshot;

UPDATE production_ingredients
SET expected_quantity = expected_quantity_grams,
    actual_quantity = actual_quantity_grams,
    measurement_unit_snapshot = 'GRAM',
    cost_per_base_unit_snapshot = cost_per_gram_snapshot;

ALTER TABLE ingredients
    MODIFY COLUMN measurement_unit VARCHAR(30) NOT NULL,
    MODIFY COLUMN current_stock DECIMAL(19,4) NOT NULL,
    MODIFY COLUMN minimum_stock DECIMAL(19,4) NOT NULL,
    MODIFY COLUMN cost_per_base_unit DECIMAL(19,6) NOT NULL;

ALTER TABLE recipe_ingredients
    MODIFY COLUMN quantity DECIMAL(19,4) NOT NULL;

ALTER TABLE production_ingredients
    MODIFY COLUMN expected_quantity DECIMAL(19,4) NOT NULL,
    MODIFY COLUMN actual_quantity DECIMAL(19,4) NULL,
    MODIFY COLUMN measurement_unit_snapshot VARCHAR(30) NOT NULL,
    MODIFY COLUMN cost_per_base_unit_snapshot DECIMAL(19,6) NOT NULL;

ALTER TABLE ingredients
    DROP CHECK ck_ingredients_current_stock,
    DROP CHECK ck_ingredients_minimum_stock,
    DROP CHECK ck_ingredients_cost_per_kilogram,
    DROP COLUMN current_stock_grams,
    DROP COLUMN minimum_stock_grams,
    DROP COLUMN cost_per_kilogram,
    ADD CONSTRAINT ck_ingredients_measurement_unit
        CHECK (measurement_unit IN ('GRAM', 'MILLILITER', 'UNIT')),
    ADD CONSTRAINT ck_ingredients_current_stock CHECK (current_stock >= 0),
    ADD CONSTRAINT ck_ingredients_minimum_stock CHECK (minimum_stock >= 0),
    ADD CONSTRAINT ck_ingredients_cost_per_base_unit CHECK (cost_per_base_unit >= 0);

ALTER TABLE recipe_ingredients
    DROP CHECK ck_recipe_ingredient_quantity,
    DROP COLUMN quantity_grams,
    ADD CONSTRAINT ck_recipe_ingredient_quantity CHECK (quantity > 0);

ALTER TABLE production_ingredients
    DROP CHECK ck_production_ingredient_expected,
    DROP CHECK ck_production_ingredient_actual,
    DROP CHECK ck_production_ingredient_cost,
    DROP COLUMN expected_quantity_grams,
    DROP COLUMN actual_quantity_grams,
    DROP COLUMN cost_per_gram_snapshot,
    ADD CONSTRAINT ck_production_ingredient_expected CHECK (expected_quantity >= 0),
    ADD CONSTRAINT ck_production_ingredient_actual
        CHECK (actual_quantity IS NULL OR actual_quantity >= 0),
    ADD CONSTRAINT ck_production_ingredient_measurement_unit
        CHECK (measurement_unit_snapshot IN ('GRAM', 'MILLILITER', 'UNIT')),
    ADD CONSTRAINT ck_production_ingredient_cost
        CHECK (cost_per_base_unit_snapshot >= 0);
