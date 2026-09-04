package com.bienCriollas.stock.variety.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

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

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "variedad_empanada")
public class EmpanadaVariety {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id_variedad")
    @JsonProperty("id_variedad")
    private Long varietyId;

    @Column(name = "nombre", nullable = false, unique = true)
    @JsonProperty("nombre")
    private String name;

    @Column(name = "precio_unitario", nullable = false)
    @JsonProperty("precioUnitario")
    private BigDecimal unitPrice;

    @Column(name = "precio_media_docena", nullable = false)
    @JsonProperty("precioMediaDocena")
    private BigDecimal halfDozenPrice;

    @Column(name = "precio_docena", nullable = false)
    @JsonProperty("precioDocena")
    private BigDecimal dozenPrice;

    @Column(name = "activo", nullable = false)
    @JsonProperty("activo")
    private Integer active;
}
