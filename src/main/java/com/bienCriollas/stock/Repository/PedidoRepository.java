package com.bienCriollas.stock.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.TipoEstado;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findByEstado(TipoEstado estado);
	
	List<Pedido> findByFechaCreacion(LocalDate fechaCreacion);
	
	 Page<Pedido> findByEstado(TipoEstado estado, Pageable pageable);
	 
	 Page<Pedido> findByEstadoAndFechaCreacion(
		        TipoEstado estado,
		        LocalDate fechaCreacion,
		        Pageable pageable
		);

	 @Query(
	            value = "SELECT SUM(p.total) " +
	                    "FROM pedido p " +
	                    "WHERE DATE(p.fecha_pedido) = :fecha " +
	                    "AND p.tipo_pago = :medio " +
	                    "AND p.estado = 'ENTREGADO'",
	            nativeQuery = true
	    )
	    Optional<BigDecimal> totalPorFechaYMedioPago(
	            @Param("fecha") LocalDate fecha,
	            @Param("medio") String medio
	    );

}
