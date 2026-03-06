package com.bienCriollas.stock.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.Model.BalanceMensual;


public interface BalanceMensualRepository extends JpaRepository<BalanceMensual, Long> {

	List<BalanceMensual> findByMesKeyBetweenOrderByMesKeyAsc(LocalDate desde, LocalDate hasta);

}
