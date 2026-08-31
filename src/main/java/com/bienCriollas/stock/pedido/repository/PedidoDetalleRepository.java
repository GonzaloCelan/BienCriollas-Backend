package com.bienCriollas.stock.pedido.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.pedido.entity.DetallePedido;


public interface PedidoDetalleRepository extends JpaRepository<DetallePedido, Long> {

	
	
	List<DetallePedido> findByPedidoIdPedido(Long idPedido);

	
	@Query("SELECT COALESCE(SUM(d.cantidad), 0) " +
		       "FROM DetallePedido d " +
		       "WHERE d.pedido.fechaCreacion = :fecha")
	Integer obtenerTotalEmpanadasVendidasEnFecha(@Param("fecha") LocalDate fecha);
	
	
	
	@Query(value = """
		    SELECT 
		        v.nombre AS variedad,
		        SUM(d.cantidad) AS total_vendidas
		    FROM pedido_detalle d
		    JOIN pedido p ON p.id_pedido = d.id_pedido
		    JOIN variedad_empanada v ON v.id_variedad = d.id_variedad
		    WHERE DATE(p.fecha_pedido) = :fecha
		      AND p.estado = :estado
		    GROUP BY v.nombre
		    ORDER BY total_vendidas DESC
		""", nativeQuery = true)
		List<Object[]> obtenerTotalEmpanadasPorVariedadEnFechaYEstado(
		        @Param("fecha") LocalDate fecha,
		        @Param("estado") String estado  
		);

}
