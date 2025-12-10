package com.bienCriollas.stock.Model;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_diaria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaDiaria {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Long idCaja;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "ingresos_efectivo", nullable = false)
    private BigDecimal ingresosEfectivo;

    @Column(name = "ingresos_transferencia", nullable = false)
    private BigDecimal ingresosTransferencia;

    @Column(name = "ingresos_pedidosya", nullable = false)
    private BigDecimal ingresosPedidosYa;

    @Column(name = "ingresos_totales", nullable = false)
    private BigDecimal ingresosTotales;

    @Column(name = "mermas", nullable = false)
    private BigDecimal mermas;

    @Column(name = "total_egresos", nullable = false)
    private BigDecimal totalEgresos;

    @Column(name = "balance_final", nullable = false)
    private BigDecimal balanceFinal;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
