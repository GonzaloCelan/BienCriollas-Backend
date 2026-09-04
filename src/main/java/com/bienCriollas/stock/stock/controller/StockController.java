package com.bienCriollas.stock.stock.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.waste.dto.EmpanadaLossDTO;
import com.bienCriollas.stock.stock.dto.StockAdjustmentDTO;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.dto.StockResponseDTO;
import com.bienCriollas.stock.stock.interfaces.IStockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v2/stock")
@Tag(name = "Stock", description = "Producción, disponibilidad, ajustes y pérdidas de stock.")
@RequiredArgsConstructor
public class StockController {
	private final IStockService stockService;
	
	// Endpoint para actualizar stock en lote
	@PostMapping("/actualizar")
    @Operation(summary = "Registrar producción en lote", description = "Crea o actualiza el stock elaborado para varias variedades.")
    public ResponseEntity<Boolean> updateStock(@RequestBody List<StockDTO> requests) {
        return ResponseEntity.ok(stockService.updateStock(requests));
    }
	
	
	// Endpoint para obtener todos los registros de stock
	@GetMapping("/obtener-stock-actual")
	@Operation(summary = "Obtener el stock actual", description = "Lista el stock disponible de todas las variedades activas.")
	public ResponseEntity<List<StockResponseDTO>> getCurrentStock() {
		List<StockResponseDTO> response = stockService.getAllStockRecords();
	    return ResponseEntity.ok(response);
	}
	
	
	// Endpoint para obtener registros de stock por variedad
	@GetMapping("/obtener-variedad/{idVariedad}")
	@Operation(summary = "Obtener stock por variedad", description = "Lista los registros de stock asociados a una variedad.")
	public ResponseEntity<List<StockResponseDTO>> getStockByVariety(
			@PathVariable("idVariedad") @Parameter(description = "ID de la variedad", example = "2") Long varietyId) {

	    List<StockResponseDTO> response = stockService.getStockRecordsByVariety(varietyId);

	    return ResponseEntity.ok(response);
	}
	
	
	// Endpoint para descontar stock de una variedad
	@PostMapping("/descontarStock/{idVariedad}/{cantidad}")
	@Operation(summary = "Descontar stock", description = "Descuenta manualmente una cantidad de la variedad indicada.")
	public ResponseEntity<String> decreaseStock(
			@PathVariable("idVariedad") @Parameter(description = "ID de la variedad", example = "2") Long varietyId,
			@PathVariable("cantidad") @Parameter(description = "Cantidad a descontar", example = "6") Integer quantity) {
	    stockService.decreaseVarietyStock(varietyId, quantity);
	    return ResponseEntity.ok("Stock descontado correctamente.");
	}
	
	
	
	// Endpoint para registar empanadas perdidas
	@PostMapping("/perdidas")
    @Operation(summary = "Registrar pérdidas", description = "Registra mermas y descuenta las unidades informadas.")
    public ResponseEntity<Void> registerLosses(@RequestBody List<EmpanadaLossDTO> losses) {
        stockService.registerLosses(losses);
        return ResponseEntity.ok().build();
    }
	
	// Endpoint para resetar stock disponible
	@PostMapping("/ajustar")
	@Operation(summary = "Ajustar stock disponible", description = "Corrige manualmente la disponibilidad actual por variedad.")
	public ResponseEntity<Void> adjustAvailableStock(
	        @RequestBody List<StockAdjustmentDTO> adjustments
	) {
	    stockService.adjustAvailableStock(adjustments);
	    return ResponseEntity.ok().build();
	}
}
