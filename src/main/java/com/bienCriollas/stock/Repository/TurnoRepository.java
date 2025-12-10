package com.bienCriollas.stock.Repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Model.Turno;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // turnos de un empleado en un rango de fechas (semana)
    List<Turno> findByEmpleadoAndFechaBetween(
            Empleado empleado,
            LocalDate desde,
            LocalDate hasta
    );

    // si alguna vez querés listar todos los turnos de una fecha:
    List<Turno> findByFecha(LocalDate fecha);

	List<Turno> findByFechaBetween(LocalDate desde, LocalDate hasta);
}