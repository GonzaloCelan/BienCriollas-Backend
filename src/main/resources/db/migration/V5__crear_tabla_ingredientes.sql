CREATE TABLE ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    current_stock_grams DECIMAL(14,2) NOT NULL DEFAULT 0,
    minimum_stock_grams DECIMAL(14,2) NOT NULL DEFAULT 0,
    cost_per_gram DECIMAL(14,6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ingredients_name UNIQUE (name),
    CONSTRAINT ck_ingredients_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_ingredients_current_stock CHECK (current_stock_grams >= 0),
    CONSTRAINT ck_ingredients_minimum_stock CHECK (minimum_stock_grams >= 0),
    CONSTRAINT ck_ingredients_cost CHECK (cost_per_gram > 0),
    INDEX idx_ingredients_active_name (active, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
