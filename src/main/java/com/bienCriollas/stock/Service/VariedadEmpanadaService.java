package com.bienCriollas.stock.Service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Model.VariedadEmpanada;
import com.bienCriollas.stock.Repository.StockRepository;
import com.bienCriollas.stock.Repository.VariedadEmpanadaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VariedadEmpanadaService {
	private final VariedadEmpanadaRepository variedadEmpanadaRepository;
	
	
	// Metodo para calcular el precio total de un pedido según la variedad y cantidad
	public BigDecimal calcularPrecioTotalPedido(Long idVariedad, Integer cantidad) {

	    // Obtengo la variedad de empanada por id y pregunto si existe
	    VariedadEmpanada empanada = variedadEmpanadaRepository.findById(idVariedad)
	            .orElseThrow(() -> new RuntimeException(
	                    "No se encontró la variedad con id " + idVariedad));

	    // Guardo los precios del catalogo de la variedad pasada por id
	    BigDecimal precioUnitario     = empanada.getPrecio_unitario();
	    BigDecimal precioMediaDocena  = empanada.getPrecio_media_docena();
	    BigDecimal precioDocena       = empanada.getPrecio_docena();

	    BigDecimal total = BigDecimal.ZERO;

	    if (cantidad == null || cantidad <= 0) {
	        return total;
	    }

	    // 1) Calcular cuántas docenas, medias docenas y unidades hay
	    int docenas   = cantidad / 12;        // cuántas docenas completas
	    int resto     = cantidad % 12;        // lo que sobra

	    int medias    = resto / 6;            // cuántas medias docenas completas
	    int unidades  = resto % 6;            // lo que sobra en unidades

	    // 2) Calcular total usando los precios de cada “pack”
	    total = total
	            .add(precioDocena.multiply(BigDecimal.valueOf(docenas)))
	            .add(precioMediaDocena.multiply(BigDecimal.valueOf(medias)))
	            .add(precioUnitario.multiply(BigDecimal.valueOf(unidades)));

	    return total;
	}
}
