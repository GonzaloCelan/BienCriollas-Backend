package com.bienCriollas.stock.Repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.Model.CajaDiaria;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {

    Optional<CajaDiaria> findByFecha(LocalDate fecha);
    
    boolean existsByFecha(LocalDate fecha);
}