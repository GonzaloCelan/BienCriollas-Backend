package com.bienCriollas.stock.merma.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.merma.interfaces.IPerdidasService;
import com.bienCriollas.stock.merma.repository.MermaRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class PerdidasService implements IPerdidasService {

	private final MermaRepository mermaRepository;

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
