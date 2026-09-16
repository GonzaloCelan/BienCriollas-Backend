package com.bienCriollas.stock.production.entity;

import java.math.BigDecimal;

import com.bienCriollas.stock.production.ingredient.entity.Ingredient;

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
    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "expected_quantity_grams", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedQuantityGrams;

    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "actual_quantity_grams", precision = 14, scale = 2)
    private BigDecimal actualQuantityGrams;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 6)
    @Column(name = "cost_per_gram_snapshot", nullable = false, precision = 14, scale = 6)
    private BigDecimal costPerGramSnapshot;
}
