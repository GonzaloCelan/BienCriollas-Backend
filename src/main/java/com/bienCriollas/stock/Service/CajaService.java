package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Dto.BalanceResponseDTO;
import com.bienCriollas.stock.Dto.CajaEstadoDTO;
import com.bienCriollas.stock.Dto.CajaMetaResponseDTO;
import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.EgresoRequestDTO;
import com.bienCriollas.stock.Dto.EstadisticaDTO;
import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.CajaEgreso;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.EstadoCaja;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Model.MermaEmpanada;
import com.bienCriollas.stock.Repository.BalanceMensualRepository;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.CajaEgresoRepository;
import com.bienCriollas.stock.Repository.EgresoRepository;
import com.bienCriollas.stock.Repository.IngresoPedidosYaRepository;
import com.bienCriollas.stock.Repository.MermaRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;
import com.bienCriollas.stock.Repository.VariedadEmpanadaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CajaService {



    private final CajaEgresoRepository cajaEgresoRepository;
    private final EstadisticaService estadisticaService;
    private final CajaDiariaRepository cajaDiariaRepository;
    private final IngresoPedidosYaRepository pedidosYa;
    private final EgresoRepository egresoRepository;
    private final BalanceMensualRepository balanceMensualRepository;
    
  
 
    @Transactional
    public CajaResponseDTO registrarIngresos(LocalDate fecha) {

        EstadisticaDTO datos = estadisticaService.obtenerEstadisticasPorFecha(fecha);

        
        // Usamos el nuevo método que nos devuelve el último registro de esa fecha
        IngresoPedidosYa pedidosYaIngreso = pedidosYa.findTopByFechaOrderByIdIngresoDesc(fecha);
        
        List<Egreso> egresos = egresoRepository.buscarPorFecha(fecha);
    	
    	BigDecimal totalEgresosDiario = BigDecimal.ZERO;

        for (Egreso e : egresos) {
            if (e.getMonto() != null) {
            	totalEgresosDiario = totalEgresosDiario.add(e.getMonto());
            }
        }
        
        BigDecimal totalMermas = datos.totalMermasImporte();
        BigDecimal totalEgresos = totalEgresosDiario.add(totalMermas);

        
        // Si no hay ningún registro para esa fecha, 'pedidosYaIngreso' será null
        BigDecimal totalPedidosYa = pedidosYaIngreso != null 
                ? pedidosYaIngreso.getMonto() 
                : null;

        // Ahora sumamos el monto de PedidosYa a la transferencia
        BigDecimal transferenciaBase = (datos.totalTransferencia() != null)
                ? datos.totalTransferencia()
                : BigDecimal.ZERO;

        BigDecimal totalTransferenciaConPedidosYa = (totalPedidosYa != null)
                ? transferenciaBase.add(totalPedidosYa)
                : transferenciaBase;

        return new CajaResponseDTO(
                datos.totalIngresos(),
                datos.totalEfectivo(),
                totalTransferenciaConPedidosYa,
                totalEgresos,
                totalPedidosYa
        );
    }


   
    
    public BalanceResponseDTO calcularBalanceDiario(LocalDate fecha) {
    	
    	EstadisticaDTO datos = estadisticaService.obtenerEstadisticasPorFecha(fecha);
    	List<Egreso> egresos = egresoRepository.buscarPorFecha(fecha);
    	
    	BigDecimal totalEgresos = BigDecimal.ZERO;

        for (Egreso e : egresos) {
            if (e.getMonto() != null) {
                totalEgresos = totalEgresos.add(e.getMonto());
            }
        }
        
        CajaResponseDTO pedidosYa = this.registrarIngresos(fecha);
        
     
      
    	BigDecimal mermasTotal = datos.totalMermasImporte();
    	BigDecimal ingresoPedidosYa =
    	        Optional.ofNullable(pedidosYa.pedidosYaLiquidacion()).orElse(BigDecimal.ZERO);

    	BigDecimal ingresoTotal = datos.totalIngresos().add(ingresoPedidosYa);
    	
    	
    	BigDecimal balanceFinal = ingresoTotal
                .subtract(totalEgresos)
                .subtract(mermasTotal);

        
        return new BalanceResponseDTO(
        		ingresoTotal,
                totalEgresos.add(mermasTotal),  
                balanceFinal
        );
    }	
    
    
   
    @Transactional
    public CajaDiaria registrarCierreDeCaja(LocalDate fecha) {

        ZoneId AR = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate hoyAR = LocalDate.now(AR);

        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        // ✅ No permitir futuro
        if (fecha.isAfter(hoyAR)) {
            throw new IllegalArgumentException("No se puede cerrar una caja de fecha futura");
        }

        // ✅ Si querés cerrar SOLO hoy, dejalo. Si querés permitir días pasados, sacá este if.
        if (!fecha.equals(hoyAR)) {
            throw new IllegalArgumentException("Solo se puede cerrar la caja del día de hoy");
        }

        // 1) Obtener caja del día (o crearla ABIERTA si no existe)
        CajaDiaria caja = cajaDiariaRepository.findByFecha(fecha).orElseGet(() -> {
            try {
                CajaDiaria nueva = CajaDiaria.builder()
                        .fecha(fecha)
                        .estadoCaja(EstadoCaja.ABIERTA)
                        .ingresosEfectivo(BigDecimal.ZERO)
                        .ingresosTransferencia(BigDecimal.ZERO)
                        .ingresosPedidosYa(BigDecimal.ZERO)
                        .ingresosTotales(BigDecimal.ZERO)
                        .mermas(BigDecimal.ZERO)
                        .totalEgresos(BigDecimal.ZERO)
                        .balanceFinal(BigDecimal.ZERO)
                        .cerradoEn(null)
                        .build();

                return cajaDiariaRepository.save(nueva);

            } catch (DataIntegrityViolationException ex) {
                return cajaDiariaRepository.findByFecha(fecha)
                        .orElseThrow(() -> new RuntimeException("No se pudo crear/obtener la caja del día " + fecha));
            }
        });

        // 2) Validar estado
        if (caja.getEstadoCaja() == EstadoCaja.CERRADA) {
            throw new RuntimeException("La caja ya está cerrada para esta fecha.");
        }
        
        EstadisticaDTO datosParaMermas = estadisticaService.obtenerEstadisticasPorFecha(fecha);
        BigDecimal totalMermasDiaria = datosParaMermas.totalMermasImporte();
        // 3) Traer datos del día
        CajaResponseDTO datos = this.registrarIngresos(fecha);
        BalanceResponseDTO total = this.calcularBalanceDiario(fecha);

        BigDecimal ingresosEfectivo = nvl(datos.ingresosEfectivo());
        BigDecimal transferBase    = nvl(datos.ingresosTransferencias());

        BigDecimal ingresosPedidosYa = datos.pedidosYaLiquidacion() != null
                ? datos.pedidosYaLiquidacion()
                : BigDecimal.ZERO;

        // ✅ PedidosYa siempre cuenta como transferencia
        BigDecimal ingresosTransferFinal = transferBase.add(ingresosPedidosYa);

        BigDecimal totalMermas  = nvl(totalMermasDiaria);
        BigDecimal totalEgresos = nvl(total.egresos());

        // ✅ Ingresos totales consistentes
        BigDecimal ingresosTotales = ingresosEfectivo.add(ingresosTransferFinal);

        // ✅ Balance final (ingresos - egresos - mermas)
        BigDecimal balanceFinal = ingresosTotales.subtract(totalEgresos).subtract(totalMermas);

        // 4) Guardar snapshot + cerrar
        caja.setIngresosEfectivo(ingresosEfectivo);
        caja.setIngresosTransferencia(ingresosTransferFinal);
        caja.setIngresosPedidosYa(ingresosPedidosYa);
        caja.setIngresosTotales(ingresosTotales);

        caja.setMermas(totalMermas);
        caja.setTotalEgresos(totalEgresos);
        caja.setBalanceFinal(balanceFinal);

        caja.setEstadoCaja(EstadoCaja.CERRADA);
        caja.setCerradoEn(LocalDateTime.now(AR)); // 👈 si tu campo se llama distinto, ajustalo
        
        
        CajaDiaria cajaGuardada = cajaDiariaRepository.save(caja);
     // ✅ Impactar balance mensual (1 fila por mes)
        LocalDate mesKey = fecha.withDayOfMonth(1);

        // tu balance final ya descuenta mermas, entonces el "egreso mensual" debería incluir egresos + mermas
        BigDecimal egresoTotalMes = totalEgresos.add(totalMermas);

        balanceMensualRepository.acumularMes(
                mesKey,
                ingresosTotales,
                egresoTotalMes,
                balanceFinal
        );
    
        return cajaGuardada;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    
    @Transactional
    public List<CajaEgreso> obtenerEgresosDelDia(LocalDate fecha) {
        return cajaEgresoRepository.obtenerEgresosDelDia(fecha);
    }
    
    @Transactional
    public IngresoPedidosYa registrarIngresoPY (PedidosYaRequestDTO request) {
    	
    	IngresoPedidosYa ingreso = IngresoPedidosYa.builder()
    				.fecha(request.fecha())
    				.monto(request.monto())
    				.build();
    	
    	return pedidosYa.save(ingreso);
    	
    }
    
    
    public CajaMetaResponseDTO obtenerMeta(LocalDate fecha) {

        return cajaDiariaRepository.findByFecha(fecha)
                .map(c -> new CajaMetaResponseDTO(
                        true,
                        c.getFecha(),
                        c.getEstadoCaja() != null ? c.getEstadoCaja().name() : null, // si es enum
                        c.getCerradoEn()
                ))
                .orElseGet(() -> new CajaMetaResponseDTO(
                        false,
                        fecha,
                        null,
                        null
                ));
    }
    
    
    //METODOS PRIVADOS
    
    private CajaDiaria resolverCajaParaEgreso(Long idCaja, LocalDate fecha) {

        // Si viene idCaja, lo validamos contra DB
        if (idCaja != null) {
            CajaDiaria caja = cajaDiariaRepository.findById(idCaja)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la caja con id " + idCaja));

            if (!caja.getFecha().equals(fecha)) {
                throw new IllegalArgumentException("La caja indicada no corresponde a la fecha " + fecha);
            }

            return caja;
        }

        // Si no viene idCaja, buscamos por fecha; si no existe, creamos ABIERTA
        return cajaDiariaRepository.findByFecha(fecha)
                .orElseGet(() -> crearCajaAbiertaSiNoExiste(fecha));
    }

    private CajaDiaria crearCajaAbiertaSiNoExiste(LocalDate fecha) {
        try {
            CajaDiaria nueva = CajaDiaria.builder()
                    .fecha(fecha)
                    .estadoCaja(EstadoCaja.ABIERTA)
                    .ingresosEfectivo(BigDecimal.ZERO)
                    .ingresosTransferencia(BigDecimal.ZERO)
                    .ingresosPedidosYa(BigDecimal.ZERO)
                    .ingresosTotales(BigDecimal.ZERO)
                    .mermas(BigDecimal.ZERO)
                    .totalEgresos(BigDecimal.ZERO)
                    .balanceFinal(BigDecimal.ZERO)
                    .build();

            return cajaDiariaRepository.save(nueva);

        } catch (DataIntegrityViolationException ex) {
            // Si entraron 2 requests al mismo tiempo y ya la creó otro, la re-lee.
            return cajaDiariaRepository.findByFecha(fecha)
                    .orElseThrow(() -> new RuntimeException("No se pudo crear/obtener la caja del día " + fecha));
        }
    }
}
