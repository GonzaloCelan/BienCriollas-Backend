package com.bienCriollas.stock.expense.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "egresos")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_egreso")
    @JsonProperty("idEgreso")
    private Long expenseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_egreso", nullable = false, length = 20)
    @JsonProperty("tipoEgreso")
    private ExpenseType expenseType;

    @Column(name = "descripcion", nullable = false, length = 255)
    @JsonProperty("descripcion")
    private String description;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    @JsonProperty("monto")
    private BigDecimal amount;

    @Column(name = "hora", nullable = false)
    @JsonProperty("hora")
    private LocalTime time;

    // DB: DEFAULT CURRENT_TIMESTAMP
    @Column(name = "creado_en", insertable = false, updatable = false)
    @JsonProperty("creadoEn")
    private LocalDateTime createdAt;
}
