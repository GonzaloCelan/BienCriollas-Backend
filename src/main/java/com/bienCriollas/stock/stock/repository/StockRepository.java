package com.bienCriollas.stock.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.bienCriollas.stock.stock.entity.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long>{

	Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, int i);

	List<Stock> findByActivo(int i);

	List<Stock> findByIdVariedad(Long idVariedad);
	
	Optional<Stock> findByIdVariedadAndActivo(Long idVariedad, int activo);

}
