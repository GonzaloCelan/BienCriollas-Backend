package com.bienCriollas.stock.Model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empleado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEmpleado;

    private String nombre;

    private Integer activo;
}
