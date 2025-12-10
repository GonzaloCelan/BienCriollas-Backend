package com.bienCriollas.stock.Model;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "caja_egreso")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaEgreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_egreso")
    private Long idEgreso;

    @Column(name = "id_caja")
    private Long idCaja; // se completa recién al cerrar la caja diaria

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
