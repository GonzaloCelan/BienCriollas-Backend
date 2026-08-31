package com.bienCriollas.stock.pedido.entity;

import java.time.OffsetDateTime;

import com.bienCriollas.stock.pedido.enums.TipoEstado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regularizacion_pedido")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegularizacionPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regularizacion")
    private Long idRegularizacion;

    @Column(name = "id_lote", nullable = false, length = 36)
    private String idLote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private TipoEstado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private TipoEstado estadoNuevo;

    @Column(name = "realizado_por", nullable = false, length = 80)
    private String realizadoPor;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(name = "realizado_en", nullable = false)
    private OffsetDateTime realizadoEn;
}
