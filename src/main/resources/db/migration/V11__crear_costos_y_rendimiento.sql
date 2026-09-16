CREATE TABLE production_cost_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    average_hourly_labor_cost DECIMAL(14,2) NOT NULL,
    energy_percentage DECIMAL(6,2) NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_production_cost_settings_labor_cost
        CHECK (average_hourly_labor_cost > 0),
    CONSTRAINT ck_production_cost_settings_energy_percentage
        CHECK (energy_percentage >= 0 AND energy_percentage <= 100)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO production_cost_settings (
    average_hourly_labor_cost,
    energy_percentage,
    updated_at
) VALUES (5000.00, 6.00, CURRENT_TIMESTAMP(6));

ALTER TABLE productions
    ADD COLUMN labor_hourly_cost_snapshot DECIMAL(14,2) NULL,
    ADD COLUMN energy_percentage_snapshot DECIMAL(6,2) NULL,
    ADD CONSTRAINT ck_productions_labor_hourly_cost_snapshot
        CHECK (labor_hourly_cost_snapshot IS NULL OR labor_hourly_cost_snapshot > 0),
    ADD CONSTRAINT ck_productions_energy_percentage_snapshot
        CHECK (
            energy_percentage_snapshot IS NULL
            OR (energy_percentage_snapshot >= 0 AND energy_percentage_snapshot <= 100)
        );
