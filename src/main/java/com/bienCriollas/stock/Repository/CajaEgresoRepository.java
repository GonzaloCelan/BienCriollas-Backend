package com.bienCriollas.stock.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Model.CajaEgreso;

public interface CajaEgresoRepository extends JpaRepository<CajaEgreso, Long> {

	
	 @Query(
	            value = "SELECT SUM(e.monto) FROM caja_egreso e WHERE e.fecha = :fecha",
	            nativeQuery = true
	    )
	    Optional<BigDecimal> totalPorFecha(@Param("fecha") LocalDate fecha);
	 
	 
	 @Query(
		        value = "SELECT * FROM caja_egreso WHERE DATE(creado_en) = :fecha ORDER BY creado_en DESC",
		        nativeQuery = true
		    )
		    List<CajaEgreso> obtenerEgresosDelDia(@Param("fecha") LocalDate fecha);
}