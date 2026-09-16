CREATE TABLE productions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    variety_id INT UNSIGNED NOT NULL,
    recipe_id BIGINT NOT NULL,
    process_id BIGINT NULL,
    production_date DATE NOT NULL,
    planned_units INT NOT NULL,
    final_units INT NULL,
    total_minutes INT NULL,
    people_count INT NULL,
    waste_units INT NOT NULL DEFAULT 0,
    waste_reason VARCHAR(250) NULL,
    notes VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    finalized_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_productions_planned_units CHECK (planned_units > 0),
    CONSTRAINT ck_productions_final_units CHECK (final_units IS NULL OR final_units >= 0),
    CONSTRAINT ck_productions_total_minutes CHECK (total_minutes IS NULL OR total_minutes > 0),
    CONSTRAINT ck_productions_people_count CHECK (people_count IS NULL OR people_count > 0),
    CONSTRAINT ck_productions_waste_units CHECK (waste_units >= 0),
    CONSTRAINT ck_productions_status CHECK (status IN ('DRAFT', 'FINALIZED', 'CANCELED')),
    CONSTRAINT fk_production_variety FOREIGN KEY (variety_id)
        REFERENCES variedad_empanada(id_variedad),
    CONSTRAINT fk_production_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipes(id),
    CONSTRAINT fk_production_process FOREIGN KEY (process_id)
        REFERENCES production_processes(id),
    INDEX idx_productions_status (status),
    INDEX idx_productions_date (production_date),
    INDEX idx_productions_variety_date (variety_id, production_date)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE production_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    expected_quantity_grams DECIMAL(14,2) NOT NULL,
    actual_quantity_grams DECIMAL(14,2) NULL,
    cost_per_gram_snapshot DECIMAL(14,6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_production_ingredient UNIQUE (production_id, ingredient_id),
    CONSTRAINT ck_production_ingredient_expected CHECK (expected_quantity_grams >= 0),
    CONSTRAINT ck_production_ingredient_actual CHECK (
        actual_quantity_grams IS NULL OR actual_quantity_grams >= 0),
    CONSTRAINT ck_production_ingredient_cost CHECK (cost_per_gram_snapshot > 0),
    CONSTRAINT fk_production_ingredient_production FOREIGN KEY (production_id)
        REFERENCES productions(id),
    CONSTRAINT fk_production_ingredient_ingredient FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(id),
    INDEX idx_production_ingredients_ingredient (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
