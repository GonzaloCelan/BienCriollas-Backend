package com.bienCriollas.stock.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Pedido {

    @Id
    @GeneratedValue(generator = "id_pedido", strategy = GenerationType.IDENTITY)
    private Long idPedido;

    // --- Cliente ---
   
    @Column(name = "nombre_cliente", nullable = false)
    private String cliente;

    // --- Tipo de venta: PARTICULAR / PEDIDOS_YA ---
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_venta", nullable = false, length = 20)
    private TipoVenta tipoVenta;

    // --- Tipo de pago: EFECTIVO / TRANSFERENCIA ---
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 20)
    private TipoPago tipoPago;

    // --- Total efectivo ---
    @Column(name = "monto_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEfectivo;
    
    // --- Total transferencia ---
    @Column(name = "monto_transferencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTransferencia;
    
    // --- Total del pedido ---
    @Column(name = "total_pedido", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPedido;

    // --- Número de pedido (solo si es PEDIDOS_YA) ---
    @Column(name = "numero_pedido_plataforma", length = 50)
    private String numeroPedidoPedidosYa;

    // --- Horario de entrega (solo si es PARTICULAR) ---
    @Column(name = "hora_entrega")
    private LocalTime horarioEntrega;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado",nullable = false)
    private TipoEstado estado;

    @Column(name = "fecha_pedido",nullable = true)
    private LocalDate fechaCreacion;
    // --- Detalle de pedido ---
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetallePedido> detalles = new ArrayList<>();

  
}