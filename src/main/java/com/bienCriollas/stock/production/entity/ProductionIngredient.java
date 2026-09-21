package com.bienCriollas.stock.production.entity;

import java.math.BigDecimal;

import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "production_ingredients",
        uniqueConstraints = @UniqueConstraint(name = "uk_production_ingredient",
                columnNames = {"production_id", "ingredient_id"}),
        indexes = @Index(name = "idx_production_ingredients_ingredient",
                columnList = "ingredient_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @NotNull
    @DecimalMin("0.0000")
    @Digits(integer = 15, fraction = 4)
    @Column(name = "expected_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQuantity;

    @DecimalMin("0.0000")
    @Digits(integer = 15, fraction = 4)
    @Column(name = "actual_quantity", precision = 19, scale = 4)
    private BigDecimal actualQuantity;

    @NotNull
    @DecimalMin("0.000000")
    @Digits(integer = 13, fraction = 6)
    @Column(name = "cost_per_base_unit_snapshot", nullable = false, precision = 19, scale = 6)
    private BigDecimal costPerBaseUnitSnapshot;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit_snapshot", nullable = false, length = 30)
    private MeasurementUnit measurementUnitSnapshot;
}
