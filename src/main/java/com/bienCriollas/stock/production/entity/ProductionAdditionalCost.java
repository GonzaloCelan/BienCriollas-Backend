package com.bienCriollas.stock.production.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.bienCriollas.stock.production.recipe.entity.RecipeAdditionalCost;
import com.bienCriollas.stock.production.recipe.enums.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "production_additional_costs", indexes =
        @Index(name = "idx_production_additional_costs_production", columnList = "production_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionAdditionalCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_additional_cost_id")
    private RecipeAdditionalCost recipeAdditionalCost;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 20)
    private AdditionalCostType costType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name_snapshot", nullable = false, length = 100)
    private String nameSnapshot;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_mode_snapshot", nullable = false, length = 20)
    private AdditionalCostCalculationMode calculationModeSnapshot;

    @NotNull
    @Column(name = "value_snapshot", nullable = false, precision = 19, scale = 6)
    private BigDecimal valueSnapshot;

    @NotNull
    @Column(name = "calculated_expected_cost_snapshot", nullable = false,
            precision = 19, scale = 6)
    private BigDecimal calculatedExpectedCostSnapshot;

    @NotNull
    @Min(1)
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
