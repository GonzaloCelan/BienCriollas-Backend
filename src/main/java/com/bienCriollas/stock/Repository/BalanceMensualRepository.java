package com.bienCriollas.stock.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.Model.BalanceMensual;
import com.bienCriollas.stock.Model.CajaEgreso;

public interface BalanceMensualRepository extends JpaRepository <BalanceMensual, LocalDate>{

	
	@Modifying
    @Query(value = """
        INSERT INTO balance_mensual (mes_key, ingresos, egresos, balance)
        VALUES (:mesKey, :ingresos, :egresos, :balance)
        ON DUPLICATE KEY UPDATE
          ingresos = ingresos + VALUES(ingresos),
          egresos  = egresos  + VALUES(egresos),
          balance  = balance  + VALUES(balance)
    """, nativeQuery = true)
    void acumularMes(
            @Param("mesKey") LocalDate mesKey,
            @Param("ingresos") BigDecimal ingresos,
            @Param("egresos") BigDecimal egresos,
            @Param("balance") BigDecimal balance
    );

	List<BalanceMensual> findByMesKeyBetweenOrderByMesKeyAsc(LocalDate desde, LocalDate hasta);
}
