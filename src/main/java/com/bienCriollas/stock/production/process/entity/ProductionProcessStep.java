package com.bienCriollas.stock.production.process.entity;

import com.bienCriollas.stock.production.process.enums.ProcessTimeType;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "production_process_steps",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_process_step_order",
                columnNames = {"process_id", "step_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionProcessStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_id", nullable = false)
    private ProductionProcess process;

    @NotNull
    @Min(1)
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotNull
    @Min(1)
    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @NotNull
    @Min(0)
    @Column(name = "required_people", nullable = false)
    private Integer requiredPeople;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "time_type", nullable = false, length = 20)
    private ProcessTimeType timeType;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;
}
