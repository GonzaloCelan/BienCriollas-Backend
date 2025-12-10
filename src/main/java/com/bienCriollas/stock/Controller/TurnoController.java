package com.bienCriollas.stock.Controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.TurnoEditRequestDTO;
import com.bienCriollas.stock.Dto.TurnoRequestDTO;
import com.bienCriollas.stock.Model.Turno;
import com.bienCriollas.stock.Service.TurnoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TurnoController {

    private final TurnoService turnoService;



    @PostMapping("/crear")
    public Turno crearTurno(@RequestBody Turno turno) {
        return turnoService.crearTurno(turno);
    }

    @PutMapping("/{idTurno}")
    public Turno editarTurno(@PathVariable Long idTurno, @RequestBody Turno turno) {
        return turnoService.editarTurno(idTurno, turno);
    }

    @DeleteMapping("/{idTurno}")
    public void eliminarTurno(@PathVariable Long idTurno) {
        turnoService.eliminarTurno(idTurno);
    }

    @GetMapping("/semana")
    public List<Turno> turnosSemana(
            @RequestParam String desde,
            @RequestParam String hasta
    ) {
        LocalDate f1 = LocalDate.parse(desde.trim());
        LocalDate f2 = LocalDate.parse(hasta.trim());
        return turnoService.obtenerTurnosSemana(f1, f2);
    }
}