package com.bienCriollas.stock.stock.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.bienCriollas.stock.stock.entity.Stock;

import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, Long>{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Stock findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(Long idVariedad, int i);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select s
			from Stock s
			where s.activo = 1
			  and s.idVariedad in :idsVariedad
			order by s.idVariedad asc
			""")
	List<Stock> findActivosParaActualizar(
			@Param("idsVariedad") Collection<Long> idsVariedad);

	List<Stock> findByActivo(int i);

	List<Stock> findByIdVariedad(Long idVariedad);
	
	Optional<Stock> findByIdVariedadAndActivo(Long idVariedad, int activo);

}
