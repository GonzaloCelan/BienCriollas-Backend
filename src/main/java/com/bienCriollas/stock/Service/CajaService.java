package com.bienCriollas.stock.Service;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.EgresosDiariosDTO;

import com.bienCriollas.stock.Dto.IngresosDiariosDTO;

import com.bienCriollas.stock.Interface.ICajaService;
import com.bienCriollas.stock.Model.CajaDiaria;



import com.bienCriollas.stock.Repository.CajaDiariaRepository;


import com.bienCriollas.stock.enums.EstadoCaja;
import com.bienCriollas.stock.enums.TipoEstado;


import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CajaService implements ICajaService {


	private final PedidoService pedidoService;
    private final CajaDiariaRepository cajaDiariaRepository;
    private final EgresoService egresoService;
    private final PerdidasService perdidasService;
    
    
  
   
    
    
    @Override
    @Transactional
    public CajaDiaria registrarCierreDeCaja(LocalDate fecha) {

        ZoneId AR = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate hoyAR = LocalDate.now(AR);

        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        if (fecha.isAfter(hoyAR)) {
            throw new IllegalArgumentException("No se puede cerrar la caja de una fecha futura");
        }

        // ✅ Verificación correcta con Optional
        Optional<CajaDiaria> cajaExistente = cajaDiariaRepository.findByFecha(fecha);

        if (cajaExistente.isPresent()) {
            throw new IllegalArgumentException("Ya existe caja para la fecha: " + fecha);
        }

        // Traer datos del día
        IngresosDiariosDTO ingresos = pedidoService.calcularIngresosDiarios(fecha, TipoEstado.ENTREGADO);

        EgresosDiariosDTO egresos = egresoService.obtenerEgresosDiarios(fecha);

        BigDecimal mermas = perdidasService.calcularMermasPorFecha(fecha);

        BigDecimal totalEgresos = egresos.totalEgresos().add(mermas);

        BigDecimal balance = ingresos.ingresosTotal().subtract(totalEgresos);

        // Crear caja cerrada con los datos calculados
        CajaDiaria nuevaCaja = CajaDiaria.builder()
                .fecha(fecha)
                .estadoCaja(EstadoCaja.CERRADA)
                .ingresosEfectivo(ingresos.ingresosEfectivo())
                .ingresosTransferencia(ingresos.ingresosTransferencia())
                .ingresosTotales(ingresos.ingresosTotal())
                .totalEgresos(totalEgresos)
                .balanceFinal(balance)
                .cerradoEn(LocalDateTime.now(AR))
                .build();

        return cajaDiariaRepository.save(nuevaCaja);
    }






	@Override
	@Transactional(readOnly = true)
	public CajaDiaria obtenerCajaPorFecha(LocalDate fecha) {
		
		Optional<CajaDiaria> cajaOpt = cajaDiariaRepository.findByFecha(fecha);
		if (cajaOpt.isPresent()) {
			return cajaOpt.get();
		} else {
			throw new IllegalArgumentException("No se encontró caja para la fecha: " + fecha);
		}
	}






	@Override
	@Transactional(readOnly = true)
	public List<CajaDiaria> obtenerCajasPorMes(int año, int mes) {
		
		return cajaDiariaRepository.findByMes(año, mes);
	}






	@Override
	@Transactional(readOnly = true)
	public List<CajaDiaria> obtenerTodasLasCajas() {
		
		return cajaDiariaRepository.findAll();
	}

    

    


    
}
