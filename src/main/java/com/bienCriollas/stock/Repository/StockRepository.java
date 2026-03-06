package com.bienCriollas.stock.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.DetallePedido;
import com.bienCriollas.stock.Model.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long>{

	Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, int i);

	List<Stock> findByActivo(int i);

	List<Stock> findByIdVariedad(Long idVariedad);

}
