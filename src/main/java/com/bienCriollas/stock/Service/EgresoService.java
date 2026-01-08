package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Dto.EgresoResponseDTO;
import com.bienCriollas.stock.Dto.EgresoTipoDTO;
import com.bienCriollas.stock.Dto.EgresosPorcentajeDTO;
import com.bienCriollas.stock.Interface.EgresoMesTotalesProjection;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.TipoEgreso;


import com.bienCriollas.stock.Repository.EgresoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EgresoService {

	
	
	private final EgresoRepository egresoRepository;
	
	
	private static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

	@Transactional
	public Egreso registrarEgreso(EgresoTipoDTO request) {

	    Egreso egreso = new Egreso();
	    egreso.setTipoEgreso(request.tipoEgreso());
	    egreso.setDescripcion(request.descripcion());
	    egreso.setMonto(request.monto());

	    egreso.setHora(LocalTime.now(ZONA_AR));
	    egreso.setCreadoEn(LocalDateTime.now(ZONA_AR));

	    return egresoRepository.save(egreso);
	}

	
	@Transactional
	public EgresoResponseDTO calcularEgresoAcumulado() {

	    YearMonth ym = YearMonth.now();
	    LocalDateTime desde = ym.atDay(1).atStartOfDay();
	    LocalDateTime hasta = ym.plusMonths(1).atDay(1).atStartOfDay(); // exclusivo

	    BigDecimal totalPersonal   = egresoRepository.totalPorTipoEntreFechas(TipoEgreso.PERSONAL, desde, hasta);
	    BigDecimal totalProduccion = egresoRepository.totalPorTipoEntreFechas(TipoEgreso.PRODUCCION, desde, hasta);
	    BigDecimal totalOtros      = egresoRepository.totalPorTipoEntreFechas(TipoEgreso.OTROS, desde, hasta);

	    // por si tu query no usa COALESCE
	    totalPersonal   = totalPersonal   == null ? BigDecimal.ZERO : totalPersonal;
	    totalProduccion = totalProduccion == null ? BigDecimal.ZERO : totalProduccion;
	    totalOtros      = totalOtros      == null ? BigDecimal.ZERO : totalOtros;

	    return new EgresoResponseDTO(totalPersonal, totalProduccion, totalOtros);
	  }
	
	@Transactional
	public List<Egreso> obtenerEgresosDeHoy() {
		  ZoneId ar = ZoneId.of("America/Argentina/Buenos_Aires");
		  LocalDate hoy = LocalDate.now(ar);

		  LocalDateTime desde = hoy.atStartOfDay();
		  LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();

		  return egresoRepository.findEgresosDelDia(desde, hasta);
		}
	
	@Transactional
	 public Page<Egreso> listarPorTipoEgreso(TipoEgreso tipo, Pageable pageable) {
		
	    return egresoRepository.findByTipoEgresoOrderByCreadoEnDesc(tipo, pageable);
	  }
	
	
	
	@Transactional
	public List<EgresosPorcentajeDTO> obtenerKpisMesActualVsAnterior() {

        List<EgresoMesTotalesProjection> rows =
                egresoRepository.obtenerTotalesMesActualYAnteriorPorTipo();

        // Mapeo lo que vino de la query a un Map<TipoEgreso, Projection>
        Map<TipoEgreso, EgresoMesTotalesProjection> map = rows.stream()
                .collect(Collectors.toMap(
                        r -> TipoEgreso.valueOf(r.getTipoEgreso()), // en DB debe venir "PERSONAL", etc
                        r -> r,
                        (a, b) -> a
                ));

        List<EgresosPorcentajeDTO> salida = new ArrayList<>();

        for (TipoEgreso tipo : TipoEgreso.values()) {
            EgresoMesTotalesProjection r = map.get(tipo);

            BigDecimal actual = (r == null || r.getTotalMesActual() == null) ? BigDecimal.ZERO : r.getTotalMesActual();
            BigDecimal anterior = (r == null || r.getTotalMesAnterior() == null) ? BigDecimal.ZERO : r.getTotalMesAnterior();

            BigDecimal pct;
            if (anterior.compareTo(BigDecimal.ZERO) == 0) {
                pct = BigDecimal.ZERO; // tu regla: si no hay noviembre, % queda en 0
            } else {
                pct = actual.subtract(anterior)
                        .multiply(new BigDecimal("100"))
                        .divide(anterior, 2, RoundingMode.HALF_UP);
            }

            salida.add(new EgresosPorcentajeDTO(tipo, actual, pct));
        }

        return salida;
    }
}
