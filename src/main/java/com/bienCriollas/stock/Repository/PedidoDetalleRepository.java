package com.bienCriollas.stock.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Model.DetallePedido;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.TipoEstado;

public interface PedidoDetalleRepository extends JpaRepository<DetallePedido, Long> {

	List<DetallePedido> findByPedidoIdPedido(Long idPedido);

	@Query(value = """
		    SELECT COALESCE(SUM(d.cantidad), 0)
		    FROM pedido_detalle d
		    JOIN pedido p ON p.id_pedido = d.id_pedido
		    WHERE DATE(p.fecha_pedido) = :fecha
		""", nativeQuery = true)
	
		Integer obtenerTotalEmpanadasVendidasEnFecha(@Param("fecha") LocalDate fecha);
	
	
	// Esta consula retorna la cantidad de empanadas vendidas por variedad en una fecha pasada por parametro
	
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
		        @Param("estado") String estado  // 👈 ahora String
		);

}
