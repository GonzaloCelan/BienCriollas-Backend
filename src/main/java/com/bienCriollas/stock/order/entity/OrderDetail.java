package com.bienCriollas.stock.order.entity;

import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Setter;

@Entity
@Table(name = "pedido_detalle")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail {

	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido_detalle")
    @JsonProperty("id_pedido_detalle")
    private Long orderDetailId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    @JsonProperty("pedido")
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_variedad", nullable = false)
    @JsonProperty("variedad")
    private EmpanadaVariety variety;

    @Column(name = "cantidad", nullable = false)
    @JsonProperty("cantidad")
    private Integer quantity;

    
}
