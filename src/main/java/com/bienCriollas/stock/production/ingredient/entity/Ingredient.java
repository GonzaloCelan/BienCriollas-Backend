package com.bienCriollas.stock.production.ingredient.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.bienCriollas.stock.production.ingredient.exception.IngredientInactiveException;

@Entity
@Table(name = "ingredients",
        uniqueConstraints = @UniqueConstraint(name = "uk_ingredients_name", columnNames = "name"),
        indexes = @Index(name = "idx_ingredients_active_name", columnList = "active, name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "current_stock_grams", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentStockGrams;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "minimum_stock_grams", nullable = false, precision = 14, scale = 2)
    private BigDecimal minimumStockGrams;

    @NotNull
    @DecimalMin("0.001")
    @Digits(integer = 11, fraction = 3)
    @Column(name = "cost_per_kilogram", nullable = false, precision = 14, scale = 3)
    private BigDecimal costPerKilogram;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    /** Regla reutilizable por las futuras recetas y producciones. */
    public void requireActive() {
        if (!Boolean.TRUE.equals(active)) {
            throw new IngredientInactiveException(id);
        }
    }
}
