package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.BalanceMensualDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.Dto.ResumenAcumuladoDTO;
import com.bienCriollas.stock.Interface.CajaAcumuladoProjection;
import com.bienCriollas.stock.Model.BalanceMensual;
import com.bienCriollas.stock.Model.CajaEgreso;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.TipoEstado;
import com.bienCriollas.stock.Repository.BalanceMensualRepository;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.IngresoPedidosYaRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ResumenHistorialService {

	private final CajaDiariaRepository cajaDiariaRepository;
	 private final BalanceMensualRepository balanceMensualRepository;
	 private final IngresoPedidosYaRepository ingresoPedidosYaRepository;
	 private final PedidoRepository pedidoRepository;
	
	 
	 @Transactional(readOnly = true)
	public ResumenAcumuladoDTO obtenerAcumuladoHistorico(Integer anio, Integer mes) {

	    LocalDate desde = null;
	    LocalDate hasta = null;

	    // Si viene año+mes => filtra por ese mes
	    if (anio != null && mes != null) {
	        desde = LocalDate.of(anio, mes, 1);
	        hasta = desde.plusMonths(1); // [desde, hasta)
	    }

	    CajaAcumuladoProjection p = cajaDiariaRepository.obtenerAcumuladoCajasCerradas(desde, hasta);

	    BigDecimal ef  = (p == null || p.getAcumuladoEfectivo() == null) ? BigDecimal.ZERO : p.getAcumuladoEfectivo();
	    BigDecimal tr  = (p == null || p.getAcumuladoTransferencia() == null) ? BigDecimal.ZERO : p.getAcumuladoTransferencia();
	    BigDecimal py  = (p == null || p.getAcumuladoPedidosya() == null) ? BigDecimal.ZERO : p.getAcumuladoPedidosya();
	    BigDecimal tot = (p == null || p.getAcumuladoTotal() == null) ? BigDecimal.ZERO : p.getAcumuladoTotal();
	    BigDecimal eg  = (p == null || p.getEgresoAcumulado() == null) ? BigDecimal.ZERO : p.getEgresoAcumulado();

	    BigDecimal bf = tot.subtract(eg);
	    return new ResumenAcumuladoDTO(ef, tr, py, tot, eg, bf);
	}
	
	 @Transactional(readOnly = true)
	public List<BalanceMensualDTO> resumenMensualGrafico(Integer anio) {

	    ZoneId AR = ZoneId.of("America/Argentina/Buenos_Aires");
	    int year = (anio != null) ? anio : LocalDate.now(AR).getYear(); // default: año actual

	    // mes_key siempre es YYYY-MM-01
	    LocalDate desde = LocalDate.of(year, 1, 1);
	    LocalDate hasta = LocalDate.of(year, 12, 1); // inclusive con BETWEEN

	    List<BalanceMensual> rows =
	            balanceMensualRepository.findByMesKeyBetweenOrderByMesKeyAsc(desde, hasta);

	    Map<LocalDate, BigDecimal> balancePorMes = rows.stream().collect(
	            java.util.stream.Collectors.toMap(
	                    BalanceMensual::getMesKey,
	                    r -> (r.getBalance() != null ? r.getBalance() : BigDecimal.ZERO)
	            )
	    );

	    Locale locale = new Locale("es", "AR");
	    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM", locale);

	    List<BalanceMensualDTO> out = new ArrayList<>(12);

	    for (int m = 1; m <= 12; m++) {
	        LocalDate mesKey = LocalDate.of(year, m, 1);
	        BigDecimal balance = balancePorMes.getOrDefault(mesKey, BigDecimal.ZERO);

	        String label = mesKey.format(fmt).replace(".", "");
	        label = label.substring(0, 1).toUpperCase() + label.substring(1);

	        out.add(new BalanceMensualDTO(label, balance));
	    }

	    return out;
	}
	
	 
	 
	@Transactional(readOnly = true)
	public List<IngresoPedidosYa> obtenerPedidosYaLiquidaciones() {
		
		List<IngresoPedidosYa> lista = ingresoPedidosYaRepository.findAll();
		if(lista.isEmpty()) {
			return null;
		}
		return lista;
	    
		
	}
	
	
}
