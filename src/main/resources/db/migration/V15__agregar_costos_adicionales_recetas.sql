CREATE TABLE recipe_additional_costs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    cost_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    calculation_mode VARCHAR(20) NOT NULL,
    cost_value DECIMAL(19,6) NOT NULL,
    sort_order INT NOT NULL,
    notes VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    unique_nonrepeatable_cost_type VARCHAR(20) GENERATED ALWAYS AS (
        CASE
            WHEN active = TRUE AND cost_type IN ('LABOR', 'PACKAGING', 'ENERGY')
                THEN cost_type
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_recipe_additional_cost_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipes(id),
    CONSTRAINT uk_recipe_nonrepeatable_additional_cost
        UNIQUE (recipe_id, unique_nonrepeatable_cost_type),
    CONSTRAINT ck_recipe_additional_cost_type
        CHECK (cost_type IN ('LABOR', 'PACKAGING', 'ENERGY', 'OTHER')),
    CONSTRAINT ck_recipe_additional_cost_mode
        CHECK (calculation_mode IN ('FIXED_TOTAL', 'PER_UNIT', 'PERCENTAGE')),
    CONSTRAINT ck_recipe_additional_cost_value CHECK (cost_value > 0),
    CONSTRAINT ck_recipe_additional_cost_percentage
        CHECK (calculation_mode <> 'PERCENTAGE' OR cost_value <= 100),
    CONSTRAINT ck_recipe_additional_cost_sort_order CHECK (sort_order > 0),
    INDEX idx_recipe_additional_costs_recipe (recipe_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE productions
    ADD COLUMN additional_costs_snapshotted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE production_additional_costs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_id BIGINT NOT NULL,
    recipe_additional_cost_id BIGINT NULL,
    cost_type VARCHAR(20) NOT NULL,
    name_snapshot VARCHAR(100) NOT NULL,
    calculation_mode_snapshot VARCHAR(20) NOT NULL,
    value_snapshot DECIMAL(19,6) NOT NULL,
    calculated_expected_cost_snapshot DECIMAL(19,6) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_production_additional_cost_production FOREIGN KEY (production_id)
        REFERENCES productions(id),
    CONSTRAINT fk_production_additional_cost_recipe_cost FOREIGN KEY (recipe_additional_cost_id)
        REFERENCES recipe_additional_costs(id),
    CONSTRAINT ck_production_additional_cost_type
        CHECK (cost_type IN ('LABOR', 'PACKAGING', 'ENERGY', 'OTHER')),
    CONSTRAINT ck_production_additional_cost_mode
        CHECK (calculation_mode_snapshot IN ('FIXED_TOTAL', 'PER_UNIT', 'PERCENTAGE')),
    CONSTRAINT ck_production_additional_cost_value CHECK (value_snapshot > 0),
    CONSTRAINT ck_production_additional_cost_expected
        CHECK (calculated_expected_cost_snapshot >= 0),
    CONSTRAINT ck_production_additional_cost_sort_order CHECK (sort_order > 0),
    INDEX idx_production_additional_costs_production (production_id),
    INDEX idx_production_additional_costs_recipe_cost (recipe_additional_cost_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
