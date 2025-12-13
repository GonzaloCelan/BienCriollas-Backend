package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Service.EmpleadoService;
import com.bienCriollas.stock.Service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/empleados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmpleadoController {


    private final EmpleadoService empleadoService;

    @GetMapping
    public List<Empleado> obtenerEmpleados() {
        return empleadoService.findAll();
    }
}
