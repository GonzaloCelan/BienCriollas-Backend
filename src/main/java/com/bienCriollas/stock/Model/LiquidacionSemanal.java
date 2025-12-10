package com.bienCriollas.stock.Model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "liquidacion_semanal")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLiquidacion;

    @ManyToOne
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    private LocalDate semanaInicio;
    private LocalDate semanaFin;

    private Double horasTotales;

    private Double pagoTotal;

    private Integer pagado; // 0 = No, 1 = Sí
}
