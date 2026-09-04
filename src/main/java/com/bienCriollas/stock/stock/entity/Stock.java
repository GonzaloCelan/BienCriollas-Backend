package com.bienCriollas.stock.stock.entity;



import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stock_empanada")
public class Stock {
	
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stock")
    private Long stockId;

    @Column(name = "id_variedad", nullable = false)
    private Long varietyId;

    @Column(name = "fecha_elaboracion", nullable = false)
    private LocalDate productionDate;

    @Column(name = "stock_total", nullable = false)
    private Integer totalStock;

    @Column(name = "stock_disponible", nullable = false)
    private Integer availableStock;

    @Column(name = "activo", nullable = false)
    private Integer active;
	
	
}
