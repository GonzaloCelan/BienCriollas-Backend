package com.bienCriollas.stock.Model;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracion_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConfig;

    private Double valorHora;
}
