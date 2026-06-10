package com.bienCriollas.stock.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
@Table(name = "merma_empanada")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MermaEmpanada {

	@Id
    @GeneratedValue(generator = "id_merma", strategy = GenerationType.IDENTITY)
    private Long idMerma;
	
	@ManyToOne
	@JoinColumn(name = "id_variedad")
    private VariedadEmpanada variedad;
    
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;


}
