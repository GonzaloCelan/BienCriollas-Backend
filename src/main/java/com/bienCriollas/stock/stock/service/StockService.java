package com.bienCriollas.stock.stock.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.waste.dto.EmpanadaLossDTO;
import com.bienCriollas.stock.waste.exception.InvalidWasteException;
import com.bienCriollas.stock.stock.dto.StockAdjustmentDTO;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.dto.StockResponseDTO;
import com.bienCriollas.stock.stock.interfaces.IStockService;
import com.bienCriollas.stock.waste.entity.EmpanadaWaste;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.exception.InvalidStockException;
import com.bienCriollas.stock.stock.exception.StockNotFoundException;
import com.bienCriollas.stock.stock.exception.InsufficientStockException;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.InactiveVarietyException;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.waste.repository.WasteRepository;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService implements IStockService {
	
	

	private final StockRepository stockRepository;
	private final EmpanadaVarietyRepository empanadaVarietyRepository;
	private final WasteRepository wasteRepository;
	
	
	// Metodo para actualizar stock en lote
	@Override
	@Transactional
	public Boolean updateStock(List<StockDTO> requests) {
		if (requests == null || requests.isEmpty()) {
			throw new InvalidStockException("La lista de stock no puede estar vacía");
		}
		for (StockDTO request : requests) {
			validateNewStock(request);
		}

	    List<StockDTO> sortedRequests = new ArrayList<>(requests);
	    sortedRequests.sort((first, second) ->
	            Long.compare(first.varietyId(), second.varietyId()));

	    for (StockDTO request : sortedRequests) {

	        // 1) Validar variedad
	        EmpanadaVariety variety = empanadaVarietyRepository.findById(request.varietyId())
	                .orElseThrow(() -> new VarietyNotFoundException(request.varietyId()));

	        if (variety.getActive() != null && variety.getActive() == 0) {
	            throw new InactiveVarietyException(request.varietyId());
	        }

	        // 2) último stock ACTIVO para esa variedad
	        Stock latestStock = stockRepository
	                .findTopByVarietyIdAndActiveOrderByProductionDateDesc(request.varietyId(), 1);

	        Integer previousAvailableStock = 0;

	        if (latestStock != null) {

	            // 🟢 CASO NUEVO: ya hay stock para ESA VARIEDAD y ESA FECHA
	            if (latestStock.getProductionDate()
	                    .equals(request.productionDate())) {

	                // sumamos la producción al mismo registro
	                Integer previousTotal = latestStock.getTotalStock() != null
	                        ? latestStock.getTotalStock()
	                        : 0;

	                Integer previousAvailable = latestStock.getAvailableStock() != null
	                        ? latestStock.getAvailableStock()
	                        : 0;

	                latestStock.setTotalStock(previousTotal + request.totalStock());
	                latestStock.setAvailableStock(previousAvailable + request.totalStock());

	                stockRepository.save(latestStock);
	                // importantísimo: pasamos al siguiente del for, NO insertamos uno nuevo
	                continue;
	            }

	            // 🟡 CASO DE SIEMPRE: último stock es de un día anterior
	            previousAvailableStock = latestStock.getAvailableStock();

	            // desactivar el anterior
	            latestStock.setActive(0);
	            stockRepository.save(latestStock);
	        }

	        // 3) nuevo disponible = sobrante + producción nueva (para un día nuevo)
	        Integer newAvailableStock = previousAvailableStock + request.totalStock();

	        // 4) create registro nuevo (para esa variedad y esa fecha)
	        Stock newStock = Stock.builder()
	                .varietyId(request.varietyId())
	                .productionDate(request.productionDate())
	                .totalStock(request.totalStock())
	                .availableStock(newAvailableStock)
	                .active(1)
	                .build();

	        stockRepository.save(newStock);
	    }

	    // si todo llegó hasta acá sin excepciones, consideramos que fue OK
	    return true;
	}

	
	// Metodo para descontar stock de una variedad
	@Override
	@Transactional
	public Boolean decreaseVarietyStock(Long varietyId, Integer quantityToDecrease) {
		if (varietyId == null) {
			throw new InvalidStockException("El id de la variedad es obligatorio");
		}
		if (quantityToDecrease == null || quantityToDecrease <= 0) {
			throw new InvalidStockException("La cantidad a descontar debe ser mayor a cero");
		}

		adjustAvailability(Map.of(varietyId, -quantityToDecrease));

	    return true;
	}

	/**
	 * Aplica en una sola transacción los cambios de disponibilidad de varias
	 * variedades. Un valor negativo descuenta y uno positivo devuelve stock.
	 * Los registros se bloquean en orden de variedad para evitar actualizaciones
	 * perdidas y deadlocks cuando llegan pedidos simultáneos.
	 */
	@Transactional
	public void adjustAvailability(Map<Long, Integer> changesByVariety) {
		if (changesByVariety == null || changesByVariety.isEmpty()) {
			return;
		}

		TreeMap<Long, Integer> sortedChanges = new TreeMap<>();
		changesByVariety.forEach((varietyId, change) -> {
			if (varietyId == null) {
				throw new InvalidStockException("El id de la variedad es obligatorio");
			}
			if (change != null && change != 0) {
				accumulateQuantity(sortedChanges, varietyId, change);
			}
		});

		if (sortedChanges.isEmpty()) {
			return;
		}

		List<Stock> lockedStocks = stockRepository
				.findActiveForUpdate(sortedChanges.keySet());
		Map<Long, Stock> stockByVariety = indexByVariety(lockedStocks);

		for (Map.Entry<Long, Integer> change : sortedChanges.entrySet()) {
			Stock stock = stockByVariety.get(change.getKey());
			if (stock == null) {
				throw new InsufficientStockException(
						"No hay stock activo para la variedad con id " + change.getKey());
			}

			int newAvailableStock = Math.addExact(stock.getAvailableStock(), change.getValue());
			if (newAvailableStock < 0) {
				throw new InsufficientStockException(
						"No hay suficiente stock disponible para la variedad con id " + change.getKey());
			}

			stock.setAvailableStock(newAvailableStock);
		}

		stockRepository.saveAll(lockedStocks);
	}
	
	
	// Metodo para obtener todos los registros de stock
	
	@Override
	@Transactional(readOnly = true)
	public List<StockResponseDTO> getAllStockRecords() {
		
		// solo los registros activos (uno por variedad)
	    List<Stock> stocks = stockRepository.findByActive(1);

	    return stocks.stream()
	            .map(s -> new StockResponseDTO(
	                    s.getVarietyId(),
	                    s.getProductionDate(),
	                    s.getTotalStock(),
	                    s.getAvailableStock()
	            ))
	            .toList();
		
		
	}
	
	// Metodo para obtener todos los registros de stock por variedad
	
	@Override
	@Transactional(readOnly = true)
	public List<StockResponseDTO> getStockRecordsByVariety(Long varietyId) {
		if (varietyId == null) {
			throw new InvalidStockException("El id de la variedad es obligatorio");
		}
		List<Stock> stocks = stockRepository.findByVarietyId(varietyId);
		if (stocks.isEmpty()) {
			throw new StockNotFoundException(varietyId);
		}
		List<StockResponseDTO> response = stocks.stream()
				.map(s -> new StockResponseDTO(
						s.getVarietyId(),
						s.getProductionDate(),
						s.getTotalStock(),
						s.getAvailableStock()
						))
				.toList();
		
		
		return response;
	
	
}
	
	
	//Metodo para registrar empandas perdidas por variedad
	@Override
	@Transactional
    public void registerLosses(List<EmpanadaLossDTO> losses) {
		if (losses == null || losses.isEmpty()) {
			throw new InvalidWasteException("La lista de pérdidas no puede estar vacía");
		}

		TreeMap<Long, Integer> changes = new TreeMap<>();
		Map<Long, EmpanadaVariety> varieties = new HashMap<>();
		List<EmpanadaLossDTO> validLosses = new ArrayList<>();

		for (EmpanadaLossDTO loss : losses) {
			if (loss == null || loss.varietyId() == null) {
				throw new InvalidWasteException("Cada pérdida debe indicar una variedad");
			}
			if (loss.quantity() == null || loss.quantity() <= 0) {
				throw new InvalidWasteException("La cantidad perdida debe ser mayor a cero");
			}

			EmpanadaVariety variety = empanadaVarietyRepository.findById(loss.varietyId())
					.orElseThrow(() -> new VarietyNotFoundException(loss.varietyId()));
			varieties.put(loss.varietyId(), variety);
			accumulateQuantity(changes, loss.varietyId(), -loss.quantity());
			validLosses.add(loss);
		}

		adjustAvailability(changes);

		List<EmpanadaWaste> records = validLosses.stream()
				.map(loss -> EmpanadaWaste.builder()
						.variety(varieties.get(loss.varietyId()))
						.recordedAt(LocalDateTime.now())
						.quantity(loss.quantity())
						.build())
				.toList();
		wasteRepository.saveAll(records);
    }


	@Override
	@Transactional
	public void adjustAvailableStock(List<StockAdjustmentDTO> adjustments) {

	    if (adjustments == null || adjustments.isEmpty()) {
	        throw new InvalidStockException("La lista de ajustes no puede estar vacía");
	    }

	    TreeMap<Long, Integer> desiredAvailability = new TreeMap<>();

	    for (StockAdjustmentDTO adjustment : adjustments) {

	        if (adjustment == null || adjustment.varietyId() == null) {
	            throw new InvalidStockException("El id de la variedad no puede ser nulo");
	        }

	        if (adjustment.availableStock() == null || adjustment.availableStock() < 0) {
	            throw new InvalidStockException(
	                "El stock disponible no puede ser nulo ni menor a 0"
	            );
	        }

	        desiredAvailability.put(adjustment.varietyId(), adjustment.availableStock());
	    }

	    List<Stock> lockedStocks = stockRepository
	            .findActiveForUpdate(desiredAvailability.keySet());
	    Map<Long, Stock> stockByVariety = indexByVariety(lockedStocks);

	    for (Map.Entry<Long, Integer> adjustment : desiredAvailability.entrySet()) {
	        Stock stock = stockByVariety.get(adjustment.getKey());
	        if (stock == null) {
	            throw new InsufficientStockException(
	                    "No existe stock activo para la variedad ID: " + adjustment.getKey());
	        }
	        stock.setAvailableStock(adjustment.getValue());
	    }

	    stockRepository.saveAll(lockedStocks);
	}

	private void accumulateQuantity(Map<Long, Integer> quantities, Long varietyId, int change) {
		Integer currentQuantity = quantities.get(varietyId);
		quantities.put(
				varietyId,
				currentQuantity == null ? change : Math.addExact(currentQuantity, change));
	}

	private void validateNewStock(StockDTO request) {
		if (request == null || request.varietyId() == null) {
			throw new InvalidStockException("Cada registro de stock debe indicar una variedad");
		}
		if (request.productionDate() == null) {
			throw new InvalidStockException("La fecha de elaboración es obligatoria");
		}
		if (request.totalStock() == null || request.totalStock() <= 0) {
			throw new InvalidStockException("El stock total debe ser mayor a cero");
		}
	}

	private Map<Long, Stock> indexByVariety(List<Stock> stocks) {
		Map<Long, Stock> stockByVariety = new HashMap<>();
		for (Stock stock : stocks) {
			stockByVariety.put(stock.getVarietyId(), stock);
		}
		return stockByVariety;
	}
}

	
