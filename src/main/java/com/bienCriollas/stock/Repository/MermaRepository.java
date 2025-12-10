package com.bienCriollas.stock.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Model.DetallePedido;
import com.bienCriollas.stock.Model.MermaEmpanada;

public interface MermaRepository  extends JpaRepository<MermaEmpanada, Long> {

	
	 @Query(value = """
		        SELECT SUM(m.cantidad)
		        FROM merma_empanada m
		        WHERE DATE(m.fecha_registro) = :fecha
		    """, nativeQuery = true)
		    Integer obtenerTotalEmpanadasPerdidasEnFecha(@Param("fecha") LocalDate fecha);
	 
	 
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
	List<Object[]> obtenerTotalMermaPorVariedadEnFecha(@Param("fecha") LocalDate fecha);
	
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
		List<Object[]> obtenerMermaPorVariedadConImporte(@Param("fecha") LocalDate fecha);


	
	@Query(value = """
	        SELECT COALESCE(SUM(m.cantidad), 0)
	        FROM merma_empanada m
	        WHERE DATE(m.fecha_registro) = :fecha
	    """, nativeQuery = true)
	    Integer totalMermasPorFecha(@Param("fecha") LocalDate fecha);
}
