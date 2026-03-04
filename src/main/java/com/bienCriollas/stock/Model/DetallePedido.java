package com.bienCriollas.stock.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido_detalle")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetallePedido {

	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_pedido_detalle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_variedad", nullable = false)
    private VariedadEmpanada variedad;

    @Column(nullable = false)
    private Integer cantidad;

    
}
