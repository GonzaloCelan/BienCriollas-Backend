package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.bienCriollas.stock.Dto.PrecioCostoVariedadDTO;
import com.bienCriollas.stock.Dto.StockDTO;
import com.bienCriollas.stock.Dto.VariedadEmpanadaDTO;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.VariedadEmpanada;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.PedidoDetalleRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;
import com.bienCriollas.stock.Repository.StockRepository;
import com.bienCriollas.stock.Repository.VariedadEmpanadaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

	
	private final VariedadEmpanadaRepository repo;
	
	@Transactional
	public List<VariedadEmpanadaDTO> actualizarPrecioCostoVariedad(List<PrecioCostoVariedadDTO> requestList) {

	    if (requestList == null || requestList.isEmpty()) return List.of();

	    List<VariedadEmpanadaDTO> response = new ArrayList<>();

	    for (PrecioCostoVariedadDTO request : requestList) {

	        // 1) Validar variedad
	        VariedadEmpanada variedad = repo.findById(request.idVariedad())
	                .orElseThrow(() -> new RuntimeException(
	                        "No se encontró la variedad con id " + request.idVariedad()));

	        if (variedad.getActivo() != null && variedad.getActivo() == 0) {
	            throw new RuntimeException("La variedad con id " + request.idVariedad() + " no está activa");
	        }

	        // 2) Validar precio
	        BigDecimal nuevoPrecio = request.precioUnitario();
	        if (nuevoPrecio == null) {
	            throw new RuntimeException("Falta precioUnitario para la variedad id " + request.idVariedad());
	        }
	        if (nuevoPrecio.compareTo(BigDecimal.ZERO) < 0) {
	            throw new RuntimeException("El precio no puede ser negativo (id " + request.idVariedad() + ")");
	        }

	        // 3) Actualizar (usa tu método del repo)
	        int actualizado = repo.actualizarPrecioUnitario(request.idVariedad(), nuevoPrecio);

	        if (actualizado != 1) {
	            throw new RuntimeException("La variedad con id " + request.idVariedad() + " no se pudo actualizar");
	        }

	        // 4) DTO de salida
	        response.add(new VariedadEmpanadaDTO(request.idVariedad(), nuevoPrecio));
	    }

	    return response;
	}
	
	
	@Transactional
	public Boolean añadirVariedadNueva(VariedadEmpanada variedad) {

		
		if(variedad == null) {
	        throw new IllegalArgumentException("La variedad no puede ser nula");
	    }
		Boolean exists = repo.existsByNombreIgnoreCase(variedad.getNombre());
		
	    if(!exists) {
	    	
	    	repo.save(variedad);
	    	return true;
	    } else {
	    	return false;
	    }
	    
		
	}
	
	@Transactional
	public VariedadEmpanada eliminarVariedad(Long idVariedad) {
		
		VariedadEmpanada variedad = repo.findById(idVariedad).orElseThrow(() -> new RuntimeException(
                "No se encontró la variedad con id " + idVariedad));
		
		if(variedad != null) {
			
	    	variedad.setActivo(0);
	    	repo.save(variedad);
	    	return variedad;
	    } else {
	    	throw new RuntimeException("No se encontró la variedad con id " + idVariedad);
	    }
	}
}