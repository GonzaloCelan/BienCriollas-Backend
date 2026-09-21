package com.bienCriollas.stock.production.ingredient.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.bienCriollas.stock.production.ingredient.exception.IngredientInactiveException;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

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
    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit", nullable = false, length = 30)
    private MeasurementUnit measurementUnit;

    @Size(max = 100)
    @Column(name = "purchase_presentation", length = 100)
    private String purchasePresentation;

    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 15, fraction = 4)
    @Column(name = "purchase_quantity", precision = 19, scale = 4)
    private BigDecimal purchaseQuantity;

    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    @Column(name = "purchase_price", precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    @NotNull
    @DecimalMin("0.0000")
    @Digits(integer = 15, fraction = 4)
    @Column(name = "current_stock", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentStock;

    @NotNull
    @DecimalMin("0.0000")
    @Digits(integer = 15, fraction = 4)
    @Column(name = "minimum_stock", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumStock;

    @NotNull
    @DecimalMin("0.000000")
    @Digits(integer = 13, fraction = 6)
    @Column(name = "cost_per_base_unit", nullable = false, precision = 19, scale = 6)
    private BigDecimal costPerBaseUnit;

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

    public boolean hasCompletePurchaseData() {
        return purchasePresentation != null && !purchasePresentation.isBlank()
                && purchaseQuantity != null && purchaseQuantity.signum() > 0
                && purchasePrice != null && purchasePrice.signum() > 0;
    }
}
