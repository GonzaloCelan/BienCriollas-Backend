package com.bienCriollas.stock.Repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Interface.CajaAcumuladoProjection;
import com.bienCriollas.stock.Model.CajaDiaria;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {

    Optional<CajaDiaria> findByFecha(LocalDate fecha);
    
    boolean existsByFecha(LocalDate fecha);
    
    @Query(value = """
    		  SELECT
    		    COALESCE(SUM(ingresos_efectivo), 0)       AS acumuladoEfectivo,
    		    COALESCE(SUM(ingresos_transferencia), 0) AS acumuladoTransferencia,
    		    COALESCE(SUM(ingresos_pedidosya), 0)     AS acumuladoPedidosya,
    		    COALESCE(SUM(ingresos_efectivo), 0)
    		    + COALESCE(SUM(ingresos_transferencia), 0)
    		    + COALESCE(SUM(ingresos_pedidosya), 0)   AS acumuladoTotal,
    		    COALESCE(SUM(mermas), 0)
    		    + COALESCE(SUM(total_egresos), 0)        AS egresoAcumulado
    		  FROM caja_diaria
    		  WHERE estado = 'CERRADA'
    		    AND fecha >= COALESCE(:desde, '1000-01-01')
    		    AND fecha <  COALESCE(:hasta, '9999-12-31')
    		""", nativeQuery = true)
    		CajaAcumuladoProjection obtenerAcumuladoCajasCerradas(
    		  @Param("desde") LocalDate desde,
    		  @Param("hasta") LocalDate hasta
    		);


}