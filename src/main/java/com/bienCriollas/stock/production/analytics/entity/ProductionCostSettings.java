package com.bienCriollas.stock.production.analytics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "production_cost_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionCostSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "average_hourly_labor_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal averageHourlyLaborCost;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "energy_percentage", nullable = false, precision = 6, scale = 2)
    private BigDecimal energyPercentage;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
