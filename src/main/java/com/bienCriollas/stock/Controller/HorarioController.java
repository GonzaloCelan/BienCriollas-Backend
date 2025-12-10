package com.bienCriollas.stock.Controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.TotalSemanaDTO;
import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Model.Turno;
import com.bienCriollas.stock.Service.EmpleadoService;
import com.bienCriollas.stock.Service.TurnoService;

@RestController
@RequestMapping("/api/horarios")
@CrossOrigin("*")
public class HorarioController {

    @Autowired
    private TurnoService turnoService;

    @Autowired
    private EmpleadoService empleadoService;
 
    @GetMapping("/totales")
    public List<TotalSemanaDTO> getTotales(
            @RequestParam String desde,
            @RequestParam String hasta
    ) {
        LocalDate f1 = LocalDate.parse(desde.trim());
        LocalDate f2 = LocalDate.parse(hasta.trim());

        List<Empleado> empleados = empleadoService.findAll();
        List<Turno> turnos = turnoService.obtenerTurnosSemana(f1, f2);

        List<TotalSemanaDTO> salida = new ArrayList<>();

        for (Empleado e : empleados) {

            double horas = 0;

            for (Turno t : turnos) {
                if (t.getEmpleado().getIdEmpleado().equals(e.getIdEmpleado())) {
                    horas += ChronoUnit.MINUTES.between(
                            t.getHoraInicio(),
                            t.getHoraFin()
                    ) / 60.0;
                }
            }

            double pago = horas * 3500;

            // ✔ Record: se construye así
            salida.add(new TotalSemanaDTO(
                    e.getNombre(),
                    horas,
                    pago
            ));
        }

        return salida;
    }

}
