package com.bienCriollas.stock.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.Dto.StockDTO;
import com.bienCriollas.stock.Dto.StockResponseDTO;
import com.bienCriollas.stock.Model.MermaEmpanada;
import com.bienCriollas.stock.Model.Stock;
import com.bienCriollas.stock.Model.VariedadEmpanada;
import com.bienCriollas.stock.Repository.MermaRepository;
import com.bienCriollas.stock.Repository.StockRepository;
import com.bienCriollas.stock.Repository.VariedadEmpanadaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

	private final StockRepository stockRepository;
	private final VariedadEmpanadaRepository variedadEmpanadaRepository;
	private final MermaRepository mermaRepository;
	
	
	// Metodo para actualizar stock en lote
	
	@Transactional
 	public Boolean actualizarStock(List<StockDTO>  requestList) {
		
		for (StockDTO request : requestList) {

	        // 1) Validar variedad
	        VariedadEmpanada variedad = variedadEmpanadaRepository.findById(request.id_variedad())
	                .orElseThrow(() -> new RuntimeException(
	                        "No se encontró la variedad con id " + request.id_variedad()));

	        if (variedad.getActivo() != null && variedad.getActivo() == 0) {
	            throw new RuntimeException("La variedad con id " + request.id_variedad() + " no está activa");
	        }

	        // 2) último stock activo para esa variedad
	        Stock ultimoStock = stockRepository
	                .findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(request.id_variedad(), 1);

	        Integer stockDisponibleAnterior = 0;

	        if (ultimoStock != null) {
	            stockDisponibleAnterior = ultimoStock.getStockDisponible();

	            // opcional: desactivar el anterior
	            ultimoStock.setActivo(0);
	            stockRepository.save(ultimoStock);
	        }

	        // 3) nuevo disponible = sobrante + producción nueva
	        Integer stockDisponibleNuevo = stockDisponibleAnterior + request.stock_total();

	        // 4) crear registro nuevo
	        Stock nuevoStock = Stock.builder()
	                .idVariedad(request.id_variedad())
	                .fechaElaboracion(request.fecha_elaboracion())
	                .stockTotal(request.stock_total())
	                .stockDisponible(stockDisponibleNuevo)
	                .activo(1)
	                .build();

	        stockRepository.save(nuevoStock);
	    }

	    // si todo llegó hasta acá sin excepciones, consideramos que fue OK
	    return true;
    }
	
	// Metodo para descontar stock de una variedad
	@Transactional
	public Boolean descontarStockVariedad(Long idVariedad, Integer cantidadADescontar) {
		
	    // 1) Obtener el último stock activo para la variedad
	    Stock ultimoStock = stockRepository
	            .findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(idVariedad, 1);

	    if (ultimoStock == null) {
	        throw new RuntimeException("No hay stock disponible para la variedad con id " + idVariedad);
	    }

	    // 2) Verificar si hay suficiente stock disponible
	    if (ultimoStock.getStockDisponible() < cantidadADescontar) {
	        throw new RuntimeException("No hay suficiente stock disponible para la variedad con id " + idVariedad);
	    }

	    // 3) Descontar la cantidad del stock disponible
	    ultimoStock.setStockDisponible(ultimoStock.getStockDisponible() - cantidadADescontar);
	    stockRepository.save(ultimoStock);

	    return true;
		
	}
	
	
	// Metodo para obtener todos los registros de stock
	
	@Transactional(readOnly = true)
	public List<StockResponseDTO> obtenerTodosLosRegistrosDeStock() {
		
		// solo los registros activos (uno por variedad)
	    List<Stock> stocks = stockRepository.findByActivo(1);

	    return stocks.stream()
	            .map(s -> new StockResponseDTO(
	                    s.getIdVariedad(),
	                    s.getFechaElaboracion(),
	                    s.getStockTotal(),
	                    s.getStockDisponible()
	            ))
	            .toList();
		
		
	}
	
	// Metodo para obtener todos los registros de stock por variedad
	
	@Transactional(readOnly = true)
	public List<StockResponseDTO> obtenerRegistrosDeStockPorVariedad(Long idVariedad) {

		
		
		List<Stock> stock = stockRepository.findByIdVariedad(idVariedad);
		List<StockResponseDTO> stockResponse = stock.stream()
				.map(s -> new StockResponseDTO(
						s.getIdVariedad(),
						s.getFechaElaboracion(),
						s.getStockTotal(),
						s.getStockDisponible()
						))
				.toList();
		
		
		return stockResponse;
	
	
}
	
	
	//Metodo para registrar empandas perdidas por variedad
	@Transactional
    public void registrarPerdidas(List<PerdidaEmpanadaDTO> perdidas) {

        for (PerdidaEmpanadaDTO perdida : perdidas) {

            // 1) Validar variedad
            var variedad = variedadEmpanadaRepository.findById(perdida.idVariedad())
                    .orElseThrow(() -> new RuntimeException(
                            "No se encontró la variedad con id " + perdida.idVariedad()));

            // 2) Último stock activo de esa variedad
            Stock stockActual = stockRepository
                    .findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(perdida.idVariedad(), 1);

            if (stockActual == null) {
                throw new RuntimeException("No hay stock cargado para esa variedad");
            }

            Integer disponible = stockActual.getStockDisponible();
            Integer aRestar = perdida.cantidad();

            if (aRestar <= 0) continue; // seguridad

            if (aRestar > disponible) {
                // Podés tirar error o ajustar:
                throw new RuntimeException("No se puede perder más de lo disponible en stock");
                // O: aRestar = disponible; // si querés permitir y dejar en 0
            }

            // 3) Actualizar stock disponible
            stockActual.setStockDisponible(disponible - aRestar);
            stockRepository.save(stockActual);

            // 4) Registrar la pérdida en la tabla empanada_perdida
            MermaEmpanada registro =  MermaEmpanada.builder()
                    .idVariedad(variedad.getId_variedad())
                    .fechaRegistro(LocalDateTime.now())
                    .cantidad(perdida.cantidad())
                    .build();

            mermaRepository.save(registro);
        }
    }
}

	

