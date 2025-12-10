package com.bienCriollas.stock.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Model.LiquidacionSemanal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiquidacionSemanalRepository extends JpaRepository<LiquidacionSemanal, Long> {

    // verificar si ya existe liquidación para ese empleado en la semana
    Optional<LiquidacionSemanal> findByEmpleadoAndSemanaInicioAndSemanaFin(
            Empleado empleado,
            LocalDate semanaInicio,
            LocalDate semanaFin
    );

    // para mostrar todas las liquidaciones de un empleado
    List<LiquidacionSemanal> findByEmpleado(Empleado empleado);
    
    
    
    List<LiquidacionSemanal> findBySemanaInicioAndSemanaFin(LocalDate semanaInicio, LocalDate semanaFin);


}
