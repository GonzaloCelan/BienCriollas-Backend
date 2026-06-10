package com.bienCriollas.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Interface.IPerdidasService;
import com.bienCriollas.stock.repository.MermaRepository;



@Service
public class PerdidasService implements IPerdidasService {

	@Autowired
	private MermaRepository mermaRepository;

	@Override
	@Transactional(readOnly = true)
	public BigDecimal calcularMermasPorFecha(LocalDate fecha) {
		
	    LocalDateTime inicio = fecha.atStartOfDay();
	    LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
	    
	    return mermaRepository.sumImporteByFecha(inicio, fin);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Object[]> obtenerMermaPorVariedadConImportePorMes(int año, int mes) {
		
			    return mermaRepository.obtenerMermaPorVariedadConImportePorMes(año, mes);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Object[]> obtenerTodasLasMermasConImporte() {
	    return mermaRepository.obtenerTodasLasMermasConImporte();
	}
}
