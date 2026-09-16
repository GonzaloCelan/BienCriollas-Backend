package com.bienCriollas.stock.production.process.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "production_processes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_production_process_variety_version",
                columnNames = {"variety_id", "version"}),
        indexes = {
                @Index(name = "idx_production_processes_variety_active",
                        columnList = "variety_id, active"),
                @Index(name = "idx_production_processes_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionProcess {

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
    @Column(name = "reference_yield_units", nullable = false)
    private Integer referenceYieldUnits;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @NotNull
    @Column(nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ProductionProcessStep> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addStep(ProductionProcessStep step) {
        steps.add(step);
        step.setProcess(this);
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
