package com.bienCriollas.stock.model;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "variedad_empanada")

public class VariedadEmpanada {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id_variedad;
	
	@Column(nullable = false, unique = true)
	private String nombre;
	
	@Column(name = "precio_unitario", nullable = false)
	private BigDecimal precioUnitario;
	
	
	@Column(nullable = false)
	private Integer activo;

}
