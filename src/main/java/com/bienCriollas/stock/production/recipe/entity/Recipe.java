package com.bienCriollas.stock.production.recipe.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "recipes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recipe_variety_version",
                columnNames = {"variety_id", "version"}),
        indexes = {
                @Index(name = "idx_recipes_variety_active", columnList = "variety_id, active"),
                @Index(name = "idx_recipes_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variety_id", nullable = false)
    private EmpanadaVariety variety;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer version;

    @NotNull
    @Min(1)
    @Column(name = "base_yield_units", nullable = false)
    private Integer baseYieldUnits;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @NotNull
    @Column(nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addIngredient(RecipeIngredient recipeIngredient) {
        ingredients.add(recipeIngredient);
        recipeIngredient.setRecipe(this);
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}

