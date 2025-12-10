package com.bienCriollas.stock.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Model.ConfiguracionPago;
import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Model.LiquidacionSemanal;
import com.bienCriollas.stock.Model.Turno;
import com.bienCriollas.stock.Repository.ConfiguracionPagoRepository;
import com.bienCriollas.stock.Repository.EmpleadoRepository;
import com.bienCriollas.stock.Repository.LiquidacionSemanalRepository;
import com.bienCriollas.stock.Repository.TurnoRepository;


import jakarta.transaction.Transactional;

import java.time.LocalDate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiquidacionService {

    private final LiquidacionSemanalRepository liquidacionRepository;
    private final TurnoRepository turnoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ConfiguracionPagoRepository configPagoRepository;
    private final TurnoService turnoService;

    // ==========================================
    // GENERAR LIQUIDACIÓN SEMANAL PARA EMPLEADO
    // ==========================================
    public LiquidacionSemanal generarLiquidacion(Long idEmpleado, LocalDate semanaInicio, LocalDate semanaFin) {

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Obtener turnos de la semana
        List<Turno> turnos = turnoRepository.findByEmpleadoAndFechaBetween(
                empleado, semanaInicio, semanaFin
        );

        // Calcular horas
        double totalHoras = turnoService.calcularHorasSemana(turnos);

        // Obtener precio por hora
        ConfiguracionPago config = configPagoRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No está configurado el valor por hora"));

        double pagoTotal = totalHoras * config.getValorHora();

        // Crear liquidación
        LiquidacionSemanal liq = LiquidacionSemanal.builder()
                .empleado(empleado)
                .semanaInicio(semanaInicio)
                .semanaFin(semanaFin)
                .horasTotales(totalHoras)
                .pagoTotal(pagoTotal)
                .pagado(0)
                .build();

        return liquidacionRepository.save(liq);
    }

    // ==========================================
    // MARCAR COMO PAGADA
    // ==========================================
    public void marcarComoPagada(Long idLiquidacion) {
        LiquidacionSemanal liq = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));

        liq.setPagado(1);
        liquidacionRepository.save(liq);
    }

    // ==========================================
    // OBTENER HISTORIAL
    // ==========================================
    public List<LiquidacionSemanal> historialEmpleado(Long idEmpleado) {

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        return liquidacionRepository.findByEmpleado(empleado);
    }
    
    @Transactional
    public void pagarSemanaCompleta(LocalDate semanaInicio) {

        LocalDate semanaFin = semanaInicio.plusDays(6);

        List<Empleado> empleados = empleadoRepository.findAll();

        for (Empleado emp : empleados) {

            // 1) Generar liquidación usando tu método EXISTENTE
            LiquidacionSemanal liq = generarLiquidacion(
                    emp.getIdEmpleado(),
                    semanaInicio,
                    semanaFin
            );

            // 2) Marcar como pagada
            marcarComoPagada(liq.getIdLiquidacion());
        }
    }
    
    public List<LiquidacionSemanal> buscarPorSemana(LocalDate inicio, LocalDate fin) {
        return liquidacionRepository.findBySemanaInicioAndSemanaFin(inicio, fin);
    }

    
   


}