package com.bienCriollas.stock.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Model.Turno;
import com.bienCriollas.stock.Repository.EmpleadoRepository;
import com.bienCriollas.stock.Repository.TurnoRepository;

import java.time.Duration;
import java.time.LocalDate;
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
    
    public double calcularHorasSemana(List<Turno> turnos) {

        double total = 0.0;

        for (Turno t : turnos) {
            LocalTime inicio = t.getHoraInicio();
            LocalTime fin = t.getHoraFin();

            double horas = Duration.between(inicio, fin).toMinutes() / 60.0;
            total += horas;
        }

        return total;
    }
}