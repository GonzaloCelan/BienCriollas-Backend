package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Dto.BalanceResponseDTO;
import com.bienCriollas.stock.Dto.CajaEstadoDTO;
import com.bienCriollas.stock.Dto.CajaResponseDTO;
import com.bienCriollas.stock.Dto.EgresoRequestDTO;
import com.bienCriollas.stock.Dto.EstadisticaDTO;
import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.CajaEgreso;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Model.MermaEmpanada;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.CajaEgresoRepository;
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

    
    
    @Transactional
     public CajaResponseDTO registrarIngresos(LocalDate fecha) {
        
        EstadisticaDTO datos = estadisticaService.obtenerEstadisticasPorFecha(fecha);

        IngresoPedidosYa pedidosYaIngreso = pedidosYa.findByFecha(fecha);
     

     // ✔ Si no hay registro para esa fecha → null
        BigDecimal totalPedidosYa = pedidosYaIngreso != null 
                ? pedidosYaIngreso.getMonto()
                : null;
        
        return new CajaResponseDTO(
            datos.totalIngresos(),
            datos.totalEfectivo(),
            datos.totalTransferencia(),
            datos.totalMermasImporte(),
            totalPedidosYa
        );
    }
    
    //METODO PARA REGISTRAR EGRESO
    @Transactional
    public CajaEgreso registrarEgreso(EgresoRequestDTO request) {
;
        CajaEgreso egreso = CajaEgreso.builder()
        		.idCaja(request.idCaja())
                .descripcion(request.descripcion())
                .monto(request.monto())
                .hora(LocalTime.now())
                .build();

        return cajaEgresoRepository.save(egreso);
    }
    
    
    public BalanceResponseDTO calcularBalanceDiario(LocalDate fecha) {
    	
    	EstadisticaDTO datos = estadisticaService.obtenerEstadisticasPorFecha(fecha);
    	List<CajaEgreso> egresos = cajaEgresoRepository.obtenerEgresosDelDia(fecha);
    	
    	BigDecimal totalEgresos = BigDecimal.ZERO;

        for (CajaEgreso e : egresos) {
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
    	
    	// 0️⃣ Validar si ya existe cierre de caja en esa fecha
        Optional<CajaDiaria> existente = cajaDiariaRepository.findByFecha(fecha);

        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe un cierre de caja registrado para esta fecha.");
        }

        // === 1️⃣ Obtener estadísticas del día (ingresos + mermas) ===
    	CajaResponseDTO datos = this.registrarIngresos(fecha);
    	BalanceResponseDTO total = this.calcularBalanceDiario(fecha);

        BigDecimal ingresosTotales = total.ingresos();
        BigDecimal ingresosEfectivo = datos.ingresosEfectivo();
        BigDecimal ingresosTransfer = datos.ingresosTransferencias();
        BigDecimal ingresosPedidosYa = datos.pedidosYaLiquidacion() != null 
                ? datos.pedidosYaLiquidacion() 
                : BigDecimal.ZERO;
        BigDecimal totalMermas = datos.totalMermas();


        BigDecimal totalEgresos = total.egresos();
        BigDecimal balanceFinal = total.balance();
        


        // === 4️⃣ Crear y guardar registro de caja_diaria ===
        CajaDiaria caja = CajaDiaria.builder()
                .fecha(fecha)
                .ingresosEfectivo(ingresosEfectivo)
                .ingresosTransferencia(ingresosTransfer)
                .ingresosPedidosYa(ingresosPedidosYa)
                .ingresosTotales(ingresosTotales)
                .mermas(totalMermas)
                .totalEgresos(totalEgresos)
                .balanceFinal(balanceFinal)
                .build();

        return cajaDiariaRepository.save(caja);
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
}
