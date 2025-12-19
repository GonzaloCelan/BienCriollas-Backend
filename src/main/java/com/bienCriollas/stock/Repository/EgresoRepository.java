package com.bienCriollas.stock.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Interface.EgresoMesTotalesProjection;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.TipoEgreso;

public interface EgresoRepository  extends JpaRepository<Egreso, Long> {

	
	@Query("""
		    SELECT COALESCE(SUM(e.monto), 0)
		    FROM Egreso e
		    WHERE e.tipoEgreso = :tipo
		      AND e.creadoEn >= :desde
		      AND e.creadoEn <  :hasta
		  """)
		  BigDecimal totalPorTipoEntreFechas(@Param("tipo") TipoEgreso tipo,
		                                    @Param("desde") LocalDateTime desde,
		                                    @Param("hasta") LocalDateTime hasta);
	
	
	@Query("""
		    SELECT e
		    FROM Egreso e
		    WHERE e.creadoEn >= :desde
		      AND e.creadoEn <  :hasta
		    ORDER BY e.creadoEn DESC
		  """)
		  List<Egreso> findEgresosDelDia(@Param("desde") LocalDateTime desde,
		                                 @Param("hasta") LocalDateTime hasta);
	
	
		Page<Egreso> findByTipoEgresoOrderByCreadoEnDesc(TipoEgreso tipoEgreso, Pageable pageable);
		
		

	    @Query("select e from Egreso e where function('date', e.creadoEn) = :fecha order by e.creadoEn desc")
	    List<Egreso> buscarPorFecha(@Param("fecha") LocalDate fecha);
	    
	    
	    @Query(value = """
	            SELECT
	              e.tipo_egreso AS tipoEgreso,
	              SUM(CASE
	                    WHEN e.creado_en >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
	                     AND e.creado_en <  DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
	                    THEN e.monto ELSE 0 END) AS totalMesActual,

	              SUM(CASE
	                    WHEN e.creado_en >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
	                     AND e.creado_en <  DATE_FORMAT(CURDATE(), '%Y-%m-01')
	                    THEN e.monto ELSE 0 END) AS totalMesAnterior
	            FROM egresos e
	            WHERE e.creado_en >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
	              AND e.creado_en <  DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
	            GROUP BY e.tipo_egreso
	            """, nativeQuery = true)
	    
	        List<EgresoMesTotalesProjection> obtenerTotalesMesActualYAnteriorPorTipo();
}
