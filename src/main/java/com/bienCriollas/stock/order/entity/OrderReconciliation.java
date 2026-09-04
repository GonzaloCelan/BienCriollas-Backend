package com.bienCriollas.stock.order.entity;

import java.time.OffsetDateTime;

import com.bienCriollas.stock.order.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class OrderReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regularizacion")
    @JsonProperty("idRegularizacion")
    private Long reconciliationId;

    @Column(name = "id_lote", nullable = false, length = 36)
    @JsonProperty("idLote")
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    @JsonProperty("pedido")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    @JsonProperty("estadoAnterior")
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    @JsonProperty("estadoNuevo")
    private OrderStatus newStatus;

    @Column(name = "realizado_por", nullable = false, length = 80)
    @JsonProperty("realizadoPor")
    private String performedBy;

    @Column(name = "motivo", nullable = false, length = 250)
    @JsonProperty("motivo")
    private String reason;

    @Column(name = "realizado_en", nullable = false)
    @JsonProperty("realizadoEn")
    private OffsetDateTime performedAt;
}
