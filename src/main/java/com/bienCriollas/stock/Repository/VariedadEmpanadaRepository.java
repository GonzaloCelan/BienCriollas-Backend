package com.bienCriollas.stock.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.VariedadEmpanada;


@Repository
public interface VariedadEmpanadaRepository extends JpaRepository<VariedadEmpanada, Long> {

	Optional<VariedadEmpanada> findById(Long idVariedad);

}
