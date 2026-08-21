package com.bienCriollas.stock.egreso.repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.egreso.interfaces.EgresoMesTotalesProjection;
import com.bienCriollas.stock.egreso.interfaces.EgresoTotalPorTipoProjection;
import com.bienCriollas.stock.egreso.entity.Egreso;
import com.bienCriollas.stock.egreso.enums.TipoEgreso;

@Repository
public interface EgresoRepository extends JpaRepository<Egreso, Long> {

    /*
     * LISTADOS POR RANGO
     * Sirve para día, mes, semana, etc.
     */
    List<Egreso> findByCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDesc(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    Page<Egreso> findByCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDesc(
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable
    );

    /*
     * LISTADO POR TIPO
     */
    Page<Egreso> findByTipoEgresoOrderByCreadoEnDesc(
            TipoEgreso tipoEgreso,
            Pageable pageable
    );

    /*
     * LISTADO POR MES + TIPO
     */
    Page<Egreso> findByTipoEgresoAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDesc(
            TipoEgreso tipoEgreso,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable
    );

    /*
     * ÚLTIMOS MOVIMIENTOS
     */
    List<Egreso> findTop5ByOrderByCreadoEnDesc();

    /*
     * TOTAL GENERAL ENTRE FECHAS
     */
    @Query("""
        SELECT COALESCE(SUM(e.monto), 0)
        FROM Egreso e
        WHERE e.creadoEn >= :desde
          AND e.creadoEn < :hasta
    """)
    BigDecimal sumarTotalEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /*
     * TOTAL POR TIPO ENTRE FECHAS
     */
    @Query("""
        SELECT COALESCE(SUM(e.monto), 0)
        FROM Egreso e
        WHERE e.tipoEgreso = :tipoEgreso
          AND e.creadoEn >= :desde
          AND e.creadoEn < :hasta
    """)
    BigDecimal sumarTotalPorTipoEntreFechas(
            @Param("tipoEgreso") TipoEgreso tipoEgreso,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /*
     * TOTALES AGRUPADOS POR TIPO
     */
    @Query("""
        SELECT 
            e.tipoEgreso AS tipoEgreso,
            COALESCE(SUM(e.monto), 0) AS total
        FROM Egreso e
        WHERE e.creadoEn >= :desde
          AND e.creadoEn < :hasta
        GROUP BY e.tipoEgreso
    """)
    List<EgresoTotalPorTipoProjection> obtenerTotalesPorTipoEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
    
    
}