package com.bienCriollas.stock.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.Stock;



@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

	// último registro de stock ACTIVO de esa variedad, ordenado por fecha descendente (ultima elaboración)
	
	 Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, Integer activo);
	 
	 List<Stock> findByIdVariedad(Long idVariedad);

	List<Stock> findByActivo(int i);

	Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, int activo);
	
	
	@Query(value = """
		      SELECT COALESCE(SUM(stock_disponible), 0)
		      FROM stock_empanada
		      WHERE activo = 1
		  """, nativeQuery = true)
		  Long totalStockDisponibleActivo();
	
}
