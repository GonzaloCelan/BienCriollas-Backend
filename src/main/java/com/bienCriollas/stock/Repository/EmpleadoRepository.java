package com.bienCriollas.stock.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.Empleado;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

}