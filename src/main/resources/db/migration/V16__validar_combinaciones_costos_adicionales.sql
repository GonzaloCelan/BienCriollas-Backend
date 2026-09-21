ALTER TABLE recipe_additional_costs
    ADD CONSTRAINT ck_recipe_additional_cost_combination CHECK (
        (cost_type = 'LABOR' AND calculation_mode = 'FIXED_TOTAL')
        OR (cost_type = 'PACKAGING' AND calculation_mode = 'PER_UNIT')
        OR (cost_type = 'ENERGY' AND calculation_mode = 'PERCENTAGE')
        OR cost_type = 'OTHER'
    );

ALTER TABLE production_additional_costs
    ADD CONSTRAINT ck_production_additional_cost_combination CHECK (
        (cost_type = 'LABOR' AND calculation_mode_snapshot = 'FIXED_TOTAL')
        OR (cost_type = 'PACKAGING' AND calculation_mode_snapshot = 'PER_UNIT')
        OR (cost_type = 'ENERGY' AND calculation_mode_snapshot = 'PERCENTAGE')
        OR cost_type = 'OTHER'
    );
