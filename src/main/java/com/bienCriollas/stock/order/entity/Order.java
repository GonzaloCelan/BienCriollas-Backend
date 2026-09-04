package com.bienCriollas.stock.order.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.fasterxml.jackson.annotation.JsonProperty;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Order {

    @Id
    @GeneratedValue(generator = "id_pedido", strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    @JsonProperty("idPedido")
    private Long orderId;

    // --- Cliente ---
   
    @Column(name = "nombre_cliente", nullable = false)
    @JsonProperty("cliente")
    private String customer;

    // --- Tipo de venta: PARTICULAR / PEDIDOS_YA ---
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_venta", nullable = false, length = 20)
    @JsonProperty("tipoVenta")
    private SaleType saleType;

    // --- Tipo de pago: EFECTIVO / TRANSFERENCIA ---
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 20)
    @JsonProperty("tipoPago")
    private PaymentType paymentType;

    // --- Total efectivo ---
    @Column(name = "monto_efectivo", nullable = false, precision = 12, scale = 2)
    @JsonProperty("montoEfectivo")
    private BigDecimal cashAmount;
    
    // --- Total transferencia ---
    @Column(name = "monto_transferencia", nullable = false, precision = 12, scale = 2)
    @JsonProperty("montoTransferencia")
    private BigDecimal transferAmount;
    
    // --- Total del pedido ---
    @Column(name = "total_pedido", nullable = false, precision = 12, scale = 2)
    @JsonProperty("totalPedido")
    private BigDecimal orderTotal;

    // --- Número de pedido (solo si es PEDIDOS_YA) ---
    @Column(name = "numero_pedido_plataforma", length = 50, nullable = true)
    @JsonProperty("numeroPedidoPedidosYa")
    private String pedidosYaOrderNumber;

    // --- Horario de entrega (solo si es PARTICULAR) ---
    @Column(name = "hora_entrega",nullable = true)
    @JsonProperty("horaEntrega")
    private LocalTime deliveryTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado",nullable = false)
    @JsonProperty("estado")
    private OrderStatus status;

    @Column(name = "fecha_pedido",nullable = true)
    @JsonProperty("fechaCreacion")
    private LocalDate creationDate;
    
    
    // --- Detalle de pedido ---
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonProperty("detalles")
    private List<OrderDetail> details = new ArrayList<>();

  
}
