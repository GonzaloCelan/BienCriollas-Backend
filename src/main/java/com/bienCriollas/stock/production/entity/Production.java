package com.bienCriollas.stock.production.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.bienCriollas.stock.production.enums.ProductionStatus;
import com.bienCriollas.stock.production.process.entity.ProductionProcess;
import com.bienCriollas.stock.production.recipe.entity.Recipe;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "productions", indexes = {
        @Index(name = "idx_productions_status", columnList = "status"),
        @Index(name = "idx_productions_date", columnList = "production_date"),
        @Index(name = "idx_productions_variety_date", columnList = "variety_id, production_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Production {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variety_id", nullable = false)
    private EmpanadaVariety variety;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id")
    private ProductionProcess process;

    @NotNull
    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @NotNull
    @Min(1)
    @Column(name = "planned_units", nullable = false)
    private Integer plannedUnits;

    @Min(0)
    @Column(name = "final_units")
    private Integer finalUnits;

    @Min(1)
    @Column(name = "total_minutes")
    private Integer totalMinutes;

    @Min(1)
    @Column(name = "people_count")
    private Integer peopleCount;

    @NotNull
    @Min(0)
    @Column(name = "waste_units", nullable = false)
    private Integer wasteUnits;

    @Size(max = 250)
    @Column(name = "waste_reason", length = 250)
    private String wasteReason;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductionStatus status;

    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ProductionIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ProductionAdditionalCost> additionalCosts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "labor_hourly_cost_snapshot", precision = 14, scale = 2)
    private BigDecimal laborHourlyCostSnapshot;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "energy_percentage_snapshot", precision = 6, scale = 2)
    private BigDecimal energyPercentageSnapshot;

    @NotNull
    @Column(name = "additional_costs_snapshotted", nullable = false)
    private Boolean additionalCostsSnapshotted;

    public void addIngredient(ProductionIngredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setProduction(this);
    }

    public void addAdditionalCost(ProductionAdditionalCost additionalCost) {
        additionalCosts.add(additionalCost);
        additionalCost.setProduction(this);
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
        if (status == null) status = ProductionStatus.DRAFT;
        if (wasteUnits == null) wasteUnits = 0;
        if (additionalCostsSnapshotted == null) additionalCostsSnapshotted = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
