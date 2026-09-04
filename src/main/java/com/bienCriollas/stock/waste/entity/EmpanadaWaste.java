package com.bienCriollas.stock.waste.entity;

import java.time.LocalDateTime;

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
@Table(name = "merma_empanada")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpanadaWaste {

	@Id
    @GeneratedValue(generator = "id_merma", strategy = GenerationType.IDENTITY)
	@Column(name = "id_merma")
	@JsonProperty("idMerma")
    private Long wasteId;
	
	@ManyToOne
	@JoinColumn(name = "id_variedad")
	@JsonProperty("variedad")
    private EmpanadaVariety variety;
    
    @Column(name = "fecha_registro", nullable = false)
	@JsonProperty("fechaRegistro")
    private LocalDateTime recordedAt;

    @Column(name = "cantidad", nullable = false)
	@JsonProperty("cantidad")
    private Integer quantity;


}
