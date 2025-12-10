package com.bienCriollas.stock.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.Stock;



@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

	// último registro de stock ACTIVO de esa variedad, ordenado por fecha descendente (ultima elaboración)
	
	 Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, Integer activo);
	 
	 List<Stock> findByIdVariedad(Long idVariedad);

	List<Stock> findByActivo(int i);

	Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, int activo);
	
	
}
