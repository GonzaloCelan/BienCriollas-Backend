package com.bienCriollas.stock.Model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "egresos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Egreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_egreso")
    private Long idEgreso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_egreso", nullable = false, length = 20)
    private TipoEgreso tipoEgreso;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    // DB: DEFAULT CURRENT_TIMESTAMP
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
