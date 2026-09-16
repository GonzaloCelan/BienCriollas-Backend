package com.bienCriollas.stock.production.recipe.entity;

import java.math.BigDecimal;

import com.bienCriollas.stock.production.ingredient.entity.Ingredient;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "recipe_ingredients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recipe_ingredient",
                columnNames = {"recipe_id", "ingredient_id"}),
        indexes = @Index(name = "idx_recipe_ingredients_ingredient", columnList = "ingredient_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "quantity_grams", nullable = false, precision = 14, scale = 2)
    private BigDecimal quantityGrams;
}

