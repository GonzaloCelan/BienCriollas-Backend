package com.bienCriollas.stock.Interface;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IPerdidasService {

	
	BigDecimal calcularMermasPorFecha(LocalDate fecha);
	
	List<Object[]> obtenerMermaPorVariedadConImportePorMes(int año, int mes);
	
	List<Object[]> obtenerTodasLasMermasConImporte();
}
