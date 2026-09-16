CREATE TABLE recipes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    variety_id INT UNSIGNED NOT NULL,
    version INT NOT NULL,
    base_yield_units INT NOT NULL,
    notes VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active_variety_id INT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN active = TRUE THEN variety_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_recipe_variety_version UNIQUE (variety_id, version),
    CONSTRAINT uk_recipe_active_variety UNIQUE (active_variety_id),
    CONSTRAINT ck_recipe_version CHECK (version > 0),
    CONSTRAINT ck_recipe_base_yield CHECK (base_yield_units > 0),
    CONSTRAINT fk_recipe_variety FOREIGN KEY (variety_id)
        REFERENCES variedad_empanada(id_variedad),
    INDEX idx_recipes_variety_active (variety_id, active),
    INDEX idx_recipes_active (active)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE recipe_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    quantity_grams DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recipe_ingredient UNIQUE (recipe_id, ingredient_id),
    CONSTRAINT ck_recipe_ingredient_quantity CHECK (quantity_grams > 0),
    CONSTRAINT fk_recipe_ingredient_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipes(id),
    CONSTRAINT fk_recipe_ingredient_ingredient FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(id),
    INDEX idx_recipe_ingredients_ingredient (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
