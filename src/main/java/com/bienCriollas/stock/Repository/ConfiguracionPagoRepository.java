package com.bienCriollas.stock.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Model.ConfiguracionPago;

@Repository
public interface ConfiguracionPagoRepository extends JpaRepository<ConfiguracionPago, Long> {

}