package com.bienCriollas.stock.Service;

import com.bienCriollas.stock.Model.Empleado;
import com.bienCriollas.stock.Repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository repo;

    public List<Empleado> findAll() {
        return repo.findAll();
    }
}