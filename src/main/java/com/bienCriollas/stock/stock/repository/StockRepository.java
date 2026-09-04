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
	Stock findTopByVarietyIdAndActiveOrderByProductionDateDesc(Long varietyId, int active);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select s
			from Stock s
			where s.active = 1
			  and s.varietyId in :varietyIds
			order by s.varietyId asc
			""")
	List<Stock> findActiveForUpdate(
			@Param("varietyIds") Collection<Long> varietyIds);

	List<Stock> findByActive(int active);

	List<Stock> findByVarietyId(Long varietyId);
	
	Optional<Stock> findByVarietyIdAndActive(Long varietyId, int active);

}
