package com.bienCriollas.stock.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Model.CajaEgreso;
import com.bienCriollas.stock.Model.IngresoPedidosYa;

public interface IngresoPedidosYaRepository extends JpaRepository<IngresoPedidosYa, Long> {

	
	@Query(
	        value = "SELECT SUM(i.monto) FROM ingreso_pedidosya i WHERE i.fecha = :fecha",
	        nativeQuery = true
	    )
	    Optional<BigDecimal> totalPorFecha(@Param("fecha") LocalDate fecha);

    IngresoPedidosYa findByFecha(LocalDate fecha);
    
    IngresoPedidosYa findTopByFechaOrderByIdIngresoDesc(LocalDate fecha);
    
    List<IngresoPedidosYa> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

}
