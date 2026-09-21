package com.bienCriollas.stock.production.recipe.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.bienCriollas.stock.production.recipe.enums.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "recipe_additional_costs", indexes =
        @Index(name = "idx_recipe_additional_costs_recipe", columnList = "recipe_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeAdditionalCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 20)
    private AdditionalCostType costType;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_mode", nullable = false, length = 20)
    private AdditionalCostCalculationMode calculationMode;

    @NotNull
    @DecimalMin("0.000001")
    @Digits(integer = 13, fraction = 6)
    @Column(name = "cost_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal value;

    @NotNull
    @Min(1)
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @NotNull
    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
        if (active == null) active = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
