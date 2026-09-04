package com.bienCriollas.stock.waste.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.waste.entity.EmpanadaWaste;



public interface WasteRepository  extends JpaRepository<EmpanadaWaste, Long> {

	
	 @Query(value = """
		        SELECT SUM(m.cantidad)
		        FROM merma_empanada m
		        WHERE DATE(m.fecha_registro) = :fecha
		    """, nativeQuery = true)
		    Integer getTotalLostEmpanadasByDate(@Param("fecha") LocalDate date);
	 
	 
	 @Query(value = """
		        SELECT 
		            v.id_variedad,
		            v.nombre AS nombre_variedad,
		            SUM(m.cantidad) AS total_merma
		        FROM merma_empanada m
		        JOIN variedad_empanada v ON v.id_variedad = m.id_variedad
		        WHERE DATE(m.fecha_registro) = :fecha
		        GROUP BY v.id_variedad, v.nombre
		        ORDER BY total_merma DESC
		    """, nativeQuery = true)
	List<Object[]> getTotalWasteByVarietyAndDate(@Param("fecha") LocalDate date);
	
	@Query(value = """
		    SELECT 
		        v.nombre              AS nombre_variedad,
		        SUM(m.cantidad)       AS total_cantidad,
		        SUM(m.cantidad * v.precio_unitario) AS total_importe
		    FROM merma_empanada m
		    JOIN variedad_empanada v ON v.id_variedad = m.id_variedad
		    WHERE DATE(m.fecha_registro) = :fecha
		    GROUP BY v.nombre
		    ORDER BY total_cantidad DESC
		""", nativeQuery = true)
		List<Object[]> getWasteValueByVariety(@Param("fecha") LocalDate date);
		
		@Query(value = """
		        SELECT
		            v.nombre              AS nombre_variedad,
		            SUM(m.cantidad)       AS total_cantidad,
		            SUM(m.cantidad * v.precio_unitario) AS total_importe
		        FROM merma_empanada m
		        JOIN variedad_empanada v ON v.id_variedad = m.id_variedad
		        GROUP BY v.nombre
		        ORDER BY total_cantidad DESC
		    """, nativeQuery = true)
		List<Object[]> getAllWasteWithValue();

		
		@Query(value = """
		        SELECT
		            v.nombre              AS nombre_variedad,
		            SUM(m.cantidad)       AS total_cantidad,
		            SUM(m.cantidad * v.precio_unitario) AS total_importe
		        FROM merma_empanada m
		        JOIN variedad_empanada v ON v.id_variedad = m.id_variedad
		        WHERE YEAR(m.fecha_registro) = :anio
		        AND MONTH(m.fecha_registro) = :mes
		        GROUP BY v.nombre
		        ORDER BY total_cantidad DESC
		    """, nativeQuery = true)
		List<Object[]> getWasteValueByVarietyAndMonth(@Param("anio") int year, @Param("mes") int month);

	
	@Query(value = """
	        SELECT COALESCE(SUM(m.cantidad), 0)
	        FROM merma_empanada m
	        WHERE DATE(m.fecha_registro) = :fecha
	    """, nativeQuery = true)
	    Integer totalWasteByDate(@Param("fecha") LocalDate date);
	
	@Query("SELECT COALESCE(SUM(m.variety.unitPrice * m.quantity), 0) " +
		       "FROM EmpanadaWaste m " +
		       "WHERE m.recordedAt >= :start AND m.recordedAt < :end")
		BigDecimal sumAmountByDate(@Param("start") LocalDateTime start,
		                           @Param("end") LocalDateTime end);
}
