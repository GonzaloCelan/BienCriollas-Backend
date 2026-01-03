package com.bienCriollas.stock.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "balance_mensual")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceMensual {

	@Id
    @Column(name = "mes_key", nullable = false)
    private LocalDate mesKey; // ej: 2026-01-01

    @Column(name = "ingresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresos = BigDecimal.ZERO;

    @Column(name = "egresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal egresos = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
