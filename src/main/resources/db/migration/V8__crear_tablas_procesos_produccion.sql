CREATE TABLE production_processes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    variety_id INT UNSIGNED NOT NULL,
    version INT NOT NULL,
    reference_yield_units INT NOT NULL,
    notes VARCHAR(1000) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active_variety_id INT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN active = TRUE THEN variety_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_production_process_variety_version UNIQUE (variety_id, version),
    CONSTRAINT uk_production_process_active_variety UNIQUE (active_variety_id),
    CONSTRAINT ck_production_process_version CHECK (version > 0),
    CONSTRAINT ck_production_process_reference_yield CHECK (reference_yield_units > 0),
    CONSTRAINT fk_production_process_variety FOREIGN KEY (variety_id)
        REFERENCES variedad_empanada(id_variedad),
    INDEX idx_production_processes_variety_active (variety_id, active),
    INDEX idx_production_processes_active (active)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE production_process_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    process_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    estimated_minutes INT NOT NULL,
    required_people INT NOT NULL,
    time_type VARCHAR(20) NOT NULL,
    notes VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_process_step_order UNIQUE (process_id, step_order),
    CONSTRAINT ck_process_step_order CHECK (step_order > 0),
    CONSTRAINT ck_process_step_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_process_step_estimated_minutes CHECK (estimated_minutes > 0),
    CONSTRAINT ck_process_step_required_people CHECK (required_people >= 0),
    CONSTRAINT ck_process_step_time_type CHECK (time_type IN ('ACTIVE', 'WAITING')),
    CONSTRAINT ck_process_step_active_people CHECK (
        time_type <> 'ACTIVE' OR required_people >= 1
    ),
    CONSTRAINT fk_process_step_process FOREIGN KEY (process_id)
        REFERENCES production_processes(id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
