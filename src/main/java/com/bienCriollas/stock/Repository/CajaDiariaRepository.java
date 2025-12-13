package com.bienCriollas.stock.Repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bienCriollas.stock.Interface.CajaAcumuladoProjection;
import com.bienCriollas.stock.Model.CajaDiaria;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {

    Optional<CajaDiaria> findByFecha(LocalDate fecha);
    
    boolean existsByFecha(LocalDate fecha);
    
    @Query(value = """
            SELECT
              SUM(COALESCE(ingresos_efectivo, 0))       AS acumuladoEfectivo,
              SUM(COALESCE(ingresos_transferencia, 0)) AS acumuladoTransferencia,
              SUM(COALESCE(ingresos_pedidosya, 0))     AS acumuladoPedidosya,
              SUM(
                COALESCE(ingresos_efectivo, 0) +
                COALESCE(ingresos_transferencia, 0) +
                COALESCE(ingresos_pedidosya, 0)
              ) AS acumuladoTotal,
              SUM(
                COALESCE(mermas, 0) +
                COALESCE(total_egresos, 0)
              ) AS egresoAcumulado
            FROM caja_diaria
            WHERE estado = 'CERRADA'
            """, nativeQuery = true)
        CajaAcumuladoProjection obtenerAcumuladoHistoricoCajasCerradas();
}