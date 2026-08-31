package com.bienCriollas.stock.stock.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.merma.dto.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.stock.dto.AjusteStockDTO;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.dto.StockResponseDTO;
import com.bienCriollas.stock.stock.interfaces.IStockService;
import com.bienCriollas.stock.merma.entity.MermaEmpanada;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.exception.StockNoDisponibleException;
import com.bienCriollas.stock.variedad.entity.VariedadEmpanada;
import com.bienCriollas.stock.merma.repository.MermaRepository;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variedad.repository.VariedadEmpanadaRepository;

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

	    List<StockDTO> solicitudesOrdenadas = new ArrayList<>(requestList);
	    solicitudesOrdenadas.sort((primera, segunda) ->
	            Long.compare(primera.id_variedad(), segunda.id_variedad()));

	    for (StockDTO request : solicitudesOrdenadas) {

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
		if (idVariedad == null) {
			throw new IllegalArgumentException("El id de la variedad es obligatorio");
		}
		if (cantidadADescontar == null || cantidadADescontar <= 0) {
			throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero");
		}

		ajustarDisponibilidad(Map.of(idVariedad, -cantidadADescontar));

	    return true;
	}

	/**
	 * Aplica en una sola transacción los cambios de disponibilidad de varias
	 * variedades. Un valor negativo descuenta y uno positivo devuelve stock.
	 * Los registros se bloquean en orden de variedad para evitar actualizaciones
	 * perdidas y deadlocks cuando llegan pedidos simultáneos.
	 */
	@Transactional
	public void ajustarDisponibilidad(Map<Long, Integer> cambiosPorVariedad) {
		if (cambiosPorVariedad == null || cambiosPorVariedad.isEmpty()) {
			return;
		}

		TreeMap<Long, Integer> cambiosOrdenados = new TreeMap<>();
		cambiosPorVariedad.forEach((idVariedad, cambio) -> {
			if (idVariedad == null) {
				throw new IllegalArgumentException("El id de la variedad es obligatorio");
			}
			if (cambio != null && cambio != 0) {
				acumularCantidad(cambiosOrdenados, idVariedad, cambio);
			}
		});

		if (cambiosOrdenados.isEmpty()) {
			return;
		}

		List<Stock> stocksBloqueados = stockRepository
				.findActivosParaActualizar(cambiosOrdenados.keySet());
		Map<Long, Stock> stockPorVariedad = indexarPorVariedad(stocksBloqueados);

		for (Map.Entry<Long, Integer> cambio : cambiosOrdenados.entrySet()) {
			Stock stock = stockPorVariedad.get(cambio.getKey());
			if (stock == null) {
				throw new StockNoDisponibleException(
						"No hay stock activo para la variedad con id " + cambio.getKey());
			}

			int disponibleNuevo = Math.addExact(stock.getStockDisponible(), cambio.getValue());
			if (disponibleNuevo < 0) {
				throw new StockNoDisponibleException(
						"No hay suficiente stock disponible para la variedad con id " + cambio.getKey());
			}

			stock.setStockDisponible(disponibleNuevo);
		}

		stockRepository.saveAll(stocksBloqueados);
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
		if (perdidas == null || perdidas.isEmpty()) {
			throw new IllegalArgumentException("La lista de pérdidas no puede estar vacía");
		}

		TreeMap<Long, Integer> cambios = new TreeMap<>();
		Map<Long, VariedadEmpanada> variedades = new HashMap<>();
		List<PerdidaEmpanadaDTO> perdidasValidas = new ArrayList<>();

		for (PerdidaEmpanadaDTO perdida : perdidas) {
			if (perdida == null || perdida.idVariedad() == null) {
				throw new IllegalArgumentException("Cada pérdida debe indicar una variedad");
			}
			if (perdida.cantidad() == null || perdida.cantidad() <= 0) {
				throw new IllegalArgumentException("La cantidad perdida debe ser mayor a cero");
			}

			VariedadEmpanada variedad = variedadEmpanadaRepository.findById(perdida.idVariedad())
					.orElseThrow(() -> new IllegalArgumentException(
							"No se encontró la variedad con id " + perdida.idVariedad()));
			variedades.put(perdida.idVariedad(), variedad);
			acumularCantidad(cambios, perdida.idVariedad(), -perdida.cantidad());
			perdidasValidas.add(perdida);
		}

		ajustarDisponibilidad(cambios);

		List<MermaEmpanada> registros = perdidasValidas.stream()
				.map(perdida -> MermaEmpanada.builder()
						.variedad(variedades.get(perdida.idVariedad()))
						.fechaRegistro(LocalDateTime.now())
						.cantidad(perdida.cantidad())
						.build())
				.toList();
		mermaRepository.saveAll(registros);
    }


	@Override
	@Transactional
	public void ajustarStockDisponible(List<AjusteStockDTO> ajustes) {

	    if (ajustes == null || ajustes.isEmpty()) {
	        throw new IllegalArgumentException("La lista de ajustes no puede estar vacía");
	    }

	    TreeMap<Long, Integer> disponibilidadDeseada = new TreeMap<>();

	    for (AjusteStockDTO ajuste : ajustes) {

	        if (ajuste.idVariedad() == null) {
	            throw new IllegalArgumentException("El id de la variedad no puede ser nulo");
	        }

	        if (ajuste.stockDisponible() == null || ajuste.stockDisponible() < 0) {
	            throw new IllegalArgumentException(
	                "El stock disponible no puede ser nulo ni menor a 0"
	            );
	        }

	        disponibilidadDeseada.put(ajuste.idVariedad(), ajuste.stockDisponible());
	    }

	    List<Stock> stocksBloqueados = stockRepository
	            .findActivosParaActualizar(disponibilidadDeseada.keySet());
	    Map<Long, Stock> stockPorVariedad = indexarPorVariedad(stocksBloqueados);

	    for (Map.Entry<Long, Integer> ajuste : disponibilidadDeseada.entrySet()) {
	        Stock stock = stockPorVariedad.get(ajuste.getKey());
	        if (stock == null) {
	            throw new StockNoDisponibleException(
	                    "No existe stock activo para la variedad ID: " + ajuste.getKey());
	        }
	        stock.setStockDisponible(ajuste.getValue());
	    }

	    stockRepository.saveAll(stocksBloqueados);
	}

	private void acumularCantidad(Map<Long, Integer> cantidades, Long idVariedad, int cambio) {
		Integer cantidadActual = cantidades.get(idVariedad);
		cantidades.put(
				idVariedad,
				cantidadActual == null ? cambio : Math.addExact(cantidadActual, cambio));
	}

	private Map<Long, Stock> indexarPorVariedad(List<Stock> stocks) {
		Map<Long, Stock> stockPorVariedad = new HashMap<>();
		for (Stock stock : stocks) {
			stockPorVariedad.put(stock.getIdVariedad(), stock);
		}
		return stockPorVariedad;
	}
}

	
