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
import com.bienCriollas.stock.Interface.ICajaService;
import com.bienCriollas.stock.Interface.IEgresoService;
import com.bienCriollas.stock.Interface.IPedidosYaService;
import com.bienCriollas.stock.Interface.IPerdidasService;
import com.bienCriollas.stock.Interface.IResumenHistoricoService;
import com.bienCriollas.stock.Model.BalanceMensual;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Repository.BalanceMensualRepository;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.IngresoPedidosYaRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;
import com.bienCriollas.stock.enums.EstadoCaja;
import com.bienCriollas.stock.enums.TipoEstado;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ResumenHistorialService implements IResumenHistoricoService {

	 private final BalanceMensualRepository balanceMensualRepository;
	 private final IngresoPedidosYaRepository ingresoPedidosYaRepository;
	 
	 private final IPedidosYaService pedidosYaService;
	 private final ICajaService cajaService;
	 private final IEgresoService egresoService;
	 private final IPerdidasService perdidasService;
	
	
	 @Override
	 @Transactional(readOnly = true)
	 public ResumenAcumuladoDTO obtenerAcumuladoHistorico(Integer año, Integer mes) {

	     BigDecimal acumuladoEfectivo = BigDecimal.ZERO;
	     BigDecimal acumuladoTransferencia = BigDecimal.ZERO;
	     BigDecimal acumuladoPedidosya = BigDecimal.ZERO;
	     BigDecimal acumuladoTotal = BigDecimal.ZERO;
	     BigDecimal egresoAcumulado = BigDecimal.ZERO;
	     BigDecimal balanceAcumulado = BigDecimal.ZERO;

	     List<CajaDiaria> cajas;
	     List<IngresoPedidosYa> liquidacionesPY;
	     List<Egreso> egresos;
	     List<Object[]> mermas;

	     if (año != null && mes != null) {
	         
	         cajas          = cajaService.obtenerCajasPorMes(año, mes);
	         liquidacionesPY = pedidosYaService.obtenerLiquidacionesPorMes(año, mes);
	         egresos        = egresoService.obtenerEgresosPorMes(año, mes);
	         mermas         = perdidasService.obtenerMermaPorVariedadConImportePorMes(año, mes);
	     } else {
	      
	         cajas          = cajaService.obtenerTodasLasCajas();
	         liquidacionesPY = pedidosYaService.obtenerTodasLasLiquidaciones();
	         egresos        = egresoService.obtenerTodosLosEgresos();
	         mermas         = perdidasService.obtenerTodasLasMermasConImporte();
	     }

	     // ── INGRESOS ──
	     acumuladoEfectivo = cajas.stream()
	             .filter(c -> c.getEstadoCaja() == EstadoCaja.CERRADA)
	             .map(CajaDiaria::getIngresosEfectivo)
	             .filter(m -> m != null)
	             .reduce(BigDecimal.ZERO, BigDecimal::add);

	     acumuladoTransferencia = cajas.stream()
	             .filter(c -> c.getEstadoCaja() == EstadoCaja.CERRADA)
	             .map(CajaDiaria::getIngresosTransferencia)
	             .filter(m -> m != null)
	             .reduce(BigDecimal.ZERO, BigDecimal::add);

	     acumuladoPedidosya = liquidacionesPY.stream()
	             .map(IngresoPedidosYa::getMonto)
	             .filter(m -> m != null)
	             .reduce(BigDecimal.ZERO, BigDecimal::add);

	     acumuladoTotal = acumuladoEfectivo.add(acumuladoTransferencia).add(acumuladoPedidosya);

	     // ── EGRESOS ──
	     egresoAcumulado = egresos.stream()
	             .map(Egreso::getMonto)
	             .filter(m -> m != null)
	             .reduce(BigDecimal.ZERO, BigDecimal::add);

	     BigDecimal totalMermas = mermas.stream()
	             .map(r -> r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO)
	             .reduce(BigDecimal.ZERO, BigDecimal::add);

	     egresoAcumulado = egresoAcumulado.add(totalMermas);

	     // ── BALANCE ──
	     balanceAcumulado = acumuladoTotal.subtract(egresoAcumulado);

	     return new ResumenAcumuladoDTO(
	             acumuladoEfectivo,
	             acumuladoTransferencia,
	             acumuladoPedidosya,
	             acumuladoTotal,
	             egresoAcumulado,
	             balanceAcumulado
	     );
	 }

	   
	@Override
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
	
	 
	@Override
	@Transactional(readOnly = true)
	public List<IngresoPedidosYa> obtenerPedidosYaLiquidaciones() {
		
		List<IngresoPedidosYa> lista = ingresoPedidosYaRepository.findAll();
		if(lista.isEmpty()) {
			return null;
		}
		return lista;
	    
		
	}
	
	
	private BigDecimal obtenerTotalAcumuladoPedidosYa() {
		BigDecimal total = BigDecimal.ZERO;
		List<IngresoPedidosYa> totalIngresosPY = ingresoPedidosYaRepository.findAll();
		
		for(IngresoPedidosYa ingreso : totalIngresosPY) {
			if(ingreso.getMonto() != null) {
				total = total.add(ingreso.getMonto());
			}
		}
		
		return total;
		
	}
	
	private BigDecimal obtenerTotalMensualPedidosYa(Integer anio, Integer mes) {
		BigDecimal total = BigDecimal.ZERO;
		
		LocalDate desde = LocalDate.of(anio, mes, 1);
		LocalDate hasta = desde.plusMonths(1); // [desde, hasta)
		
		List<IngresoPedidosYa> totalIngresosPY = ingresoPedidosYaRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta);
		
		for(IngresoPedidosYa ingreso : totalIngresosPY) {
			if(ingreso.getMonto() != null) {
				total = total.add(ingreso.getMonto());
			}
		}
		
		return total;
		
	}
	
}
