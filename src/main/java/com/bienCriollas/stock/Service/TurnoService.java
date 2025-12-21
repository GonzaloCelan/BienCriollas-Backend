package com.bienCriollas.stock.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Model.Turno;
import com.bienCriollas.stock.Repository.EmpleadoRepository;
import com.bienCriollas.stock.Repository.TurnoRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final EmpleadoRepository empleadoRepository;

 // CREAR
    public Turno crearTurno(Turno turno) {

        Long idEmpleado = turno.getEmpleado().getIdEmpleado();

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        turno.setEmpleado(empleado);
        return turnoRepository.save(turno);
    }

    // EDITAR SOLO HORAS
    public Turno editarTurno(Long idTurno, Turno datos) {

        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        turno.setHoraInicio(datos.getHoraInicio());
        turno.setHoraFin(datos.getHoraFin());

        return turnoRepository.save(turno);
    }

    // ELIMINAR
    public void eliminarTurno(Long idTurno) {
        turnoRepository.deleteById(idTurno);
    }

    // OBTENER SEMANA
    public List<Turno> obtenerTurnosSemana(LocalDate desde, LocalDate hasta) {
        return turnoRepository.findByFechaBetween(desde, hasta);
    }
    
    // CALCULAR HORAS TRABAJADAS
    public double calcularHorasSemana(List<Turno> turnos) {
        double total = 0.0;

        for (Turno t : turnos) {
            if (t.getFecha() == null || t.getHoraInicio() == null || t.getHoraFin() == null) continue;
            
            
            LocalDateTime inicio = LocalDateTime.of(t.getFecha(), t.getHoraInicio());
            LocalDateTime fin = LocalDateTime.of(t.getFecha(), t.getHoraFin());

            // Si termina "antes" o igual, es al día siguiente
            if (!fin.isAfter(inicio)) {
                fin = fin.plusDays(1);
            }

            double horas = Duration.between(inicio, fin).toMinutes() / 60.0;
            total += horas;
            
            System.out.println(
            	    "Turno: " + t.getFecha() + " " + t.getHoraInicio() + " - " + t.getHoraFin()
            	);
            	System.out.println(
            	    "Horas calculadas: " + (Duration.between(inicio, fin).toMinutes() / 60.0)
            	);
        }

        return total;
    }


}