package com.bienCriollas.stock.Model;



import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "variedad_empanada")

public class VariedadEmpanada {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id_variedad;
	
	@Column(nullable = false, unique = true)
	private String nombre;
	
	@Column(nullable = false)
	private BigDecimal precio_unitario;
	
	@Column(nullable = true)
	private BigDecimal precio_media_docena;
	
	@Column(nullable = true)
	private BigDecimal precio_docena;
	
	@Column(nullable = false)
	private Integer activo;

}
