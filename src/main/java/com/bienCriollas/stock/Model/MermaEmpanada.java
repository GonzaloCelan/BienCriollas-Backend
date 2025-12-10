package com.bienCriollas.stock.Model;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "merma_empanada")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MermaEmpanada {

	@Id
    @GeneratedValue(generator = "id_merma", strategy = GenerationType.IDENTITY)
    private Long idMerma;
	
    @Column(name = "id_variedad", nullable = false)
    private Long idVariedad;
    
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;


}
