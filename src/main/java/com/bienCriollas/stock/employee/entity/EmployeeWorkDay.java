package com.bienCriollas.stock.employee.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "employee_work_days",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_work_day_employee_date",
                columnNames = {"employee_id", "work_date"}),
        indexes = {
                @Index(name = "idx_employee_work_days_date", columnList = "work_date"),
                @Index(name = "idx_employee_work_days_employee_date",
                        columnList = "employee_id, work_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeWorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    @Column(name = "hourly_rate_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal hourlyRateSnapshot;

    @NotNull
    @Positive
    @Column(name = "total_worked_minutes", nullable = false)
    private Integer totalWorkedMinutes;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @NotEmpty
    @OneToMany(mappedBy = "workDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<@Valid EmployeeWorkShift> shifts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void replaceShifts(List<EmployeeWorkShift> newShifts) {
        shifts.clear();
        newShifts.forEach(this::addShift);
    }

    public void addShift(EmployeeWorkShift shift) {
        shifts.add(shift);
        shift.setWorkDay(this);
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
