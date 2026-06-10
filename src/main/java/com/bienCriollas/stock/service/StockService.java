package com.bienCriollas.stock.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.mermas.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.Dto.stocks.AjusteStockDTO;
import com.bienCriollas.stock.Dto.stocks.StockDTO;
import com.bienCriollas.stock.Dto.stocks.StockResponseDTO;
import com.bienCriollas.stock.Interface.IStockService;
import com.bienCriollas.stock.model.MermaEmpanada;
import com.bienCriollas.stock.model.Stock;
import com.bienCriollas.stock.model.VariedadEmpanada;
import com.bienCriollas.stock.repository.MermaRepository;
import com.bienCriollas.stock.repository.StockRepository;
import com.bienCriollas.stock.repository.VariedadEmpanadaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService implements IStockService {
	
	

	private final StockRepository stockRepository;
	private final VariedadEmpanadaRepository variedadEmpanadaRepository;
	private final MermaRepository mermaRepository;
	
	
	// Metodo para actualizar stock en lote
	@Override
	@Transactional
	public Boolean actualizarStock(List<StockDTO> requestList) {

	    for (StockDTO request : requestList) {

	        // 1) Validar variedad
	        VariedadEmpanada variedad = variedadEmpanadaRepository.findById(request.id_variedad())
	                .orElseThrow(() -> new RuntimeException(
	                        "No se encontró la variedad con id " + request.id_variedad()));

	        if (variedad.getActivo() != null && variedad.getActivo() == 0) {
	            throw new RuntimeException("La variedad con id " + request.id_variedad() + " no está activa");
	        }

	        // 2) último stock ACTIVO para esa variedad
	        Stock ultimoStock = stockRepository
	                .findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(request.id_variedad(), 1);

	        Integer stockDisponibleAnterior = 0;

	        if (ultimoStock != null) {

	            // 🟢 CASO NUEVO: ya hay stock para ESA VARIEDAD y ESA FECHA
	            if (ultimoStock.getFechaElaboracion()
	                    .equals(request.fecha_elaboracion())) {

	                // sumamos la producción al mismo registro
	                Integer totalAnterior = ultimoStock.getStockTotal() != null
	                        ? ultimoStock.getStockTotal()
	                        : 0;

	                Integer disponibleAnterior = ultimoStock.getStockDisponible() != null
	                        ? ultimoStock.getStockDisponible()
	                        : 0;

	                ultimoStock.setStockTotal(totalAnterior + request.stock_total());
	                ultimoStock.setStockDisponible(disponibleAnterior + request.stock_total());

	                stockRepository.save(ultimoStock);
	                // importantísimo: pasamos al siguiente del for, NO insertamos uno nuevo
	                continue;
	            }

	            // 🟡 CASO DE SIEMPRE: último stock es de un día anterior
	            stockDisponibleAnterior = ultimoStock.getStockDisponible();

	            // desactivar el anterior
	            ultimoStock.setActivo(0);
	            stockRepository.save(ultimoStock);
	        }

	        // 3) nuevo disponible = sobrante + producción nueva (para un día nuevo)
	        Integer stockDisponibleNuevo = stockDisponibleAnterior + request.stock_total();

	        // 4) crear registro nuevo (para esa variedad y esa fecha)
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
	@Override
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
	
	@Override
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
	
	@Override
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
	@Override
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
                    .variedad(variedad)
                    .fechaRegistro(LocalDateTime.now())
                    .cantidad(perdida.cantidad())
                    .build();

            mermaRepository.save(registro);
        }
    }


	@Override
	@Transactional
	public void ajustarStockDisponible(List<AjusteStockDTO> ajustes) {

	    if (ajustes == null || ajustes.isEmpty()) {
	        throw new IllegalArgumentException("La lista de ajustes no puede estar vacía");
	    }

	    for (AjusteStockDTO ajuste : ajustes) {

	        if (ajuste.idVariedad() == null) {
	            throw new IllegalArgumentException("El id de la variedad no puede ser nulo");
	        }

	        if (ajuste.stockDisponible() == null || ajuste.stockDisponible() < 0) {
	            throw new IllegalArgumentException(
	                "El stock disponible no puede ser nulo ni menor a 0"
	            );
	        }

	        Stock stock = stockRepository
	                .findByIdVariedadAndActivo(ajuste.idVariedad(), 1)
	                .orElseThrow(() -> new RuntimeException(
	                    "No existe stock activo para la variedad ID: " + ajuste.idVariedad()
	                ));

	        stock.setStockDisponible(ajuste.stockDisponible());

	        stockRepository.save(stock);
	    }
	}
}

	

