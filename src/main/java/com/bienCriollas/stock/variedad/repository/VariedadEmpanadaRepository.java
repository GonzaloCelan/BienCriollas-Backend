package com.bienCriollas.stock.variedad.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.variedad.entity.VariedadEmpanada;

import jakarta.transaction.Transactional;


public interface VariedadEmpanadaRepository extends JpaRepository<VariedadEmpanada, Long> {

	Optional<VariedadEmpanada> findById(Long idVariedad);
	
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE VariedadEmpanada v
           SET v.precioUnitario = :precioUnitario
         WHERE v.id_variedad = :idVariedad
    """)
    int actualizarPrecioUnitario(@Param("idVariedad") Long idVariedad,
                                 @Param("precioUnitario") BigDecimal precioUnitario);


	Boolean existsByNombreIgnoreCase(String nombre);


	List<VariedadEmpanada> findByActivo(int i);


	Optional<VariedadEmpanada> findByNombre(String nombre);
}
