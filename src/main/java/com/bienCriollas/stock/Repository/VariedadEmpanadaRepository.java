package com.bienCriollas.stock.Repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.VariedadEmpanada;

import jakarta.transaction.Transactional;


@Repository
public interface VariedadEmpanadaRepository extends JpaRepository<VariedadEmpanada, Long> {

	Optional<VariedadEmpanada> findById(Long idVariedad);
	
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE VariedadEmpanada v
           SET v.precio_unitario = :precioUnitario
         WHERE v.id_variedad = :idVariedad
    """)
    int actualizarPrecioUnitario(@Param("idVariedad") Long idVariedad,
                                 @Param("precioUnitario") BigDecimal precioUnitario);
}
