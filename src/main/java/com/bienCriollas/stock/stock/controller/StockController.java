package com.bienCriollas.stock.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.merma.dto.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.stock.dto.AjusteStockDTO;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.dto.StockResponseDTO;
import com.bienCriollas.stock.stock.interfaces.IStockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/v2/stock")
@Tag(name = "Stock", description = "Producción, disponibilidad, ajustes y pérdidas de stock.")
public class StockController {
	
	@Autowired
	private IStockService stockService;
	
	// Endpoint para actualizar stock en lote
	@PostMapping("/actualizar")
    @Operation(summary = "Registrar producción en lote", description = "Crea o actualiza el stock elaborado para varias variedades.")
    public ResponseEntity<?> actualizarStock(@RequestBody List<StockDTO> requestList) {
        try {
            Boolean ok = stockService.actualizarStock(requestList);
            return ResponseEntity.ok(ok);
        } catch (Exception e) {
            e.printStackTrace(); // para verlo en logs de Railway
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ERROR en actualizarStock: " + e.getMessage());
        }
    }
	
	
	// Endpoint para obtener todos los registros de stock
	@GetMapping("/obtener-stock-actual")
	@Operation(summary = "Obtener el stock actual", description = "Lista el stock disponible de todas las variedades activas.")
	public ResponseEntity<List<StockResponseDTO>> obetenerStockLote() {
		List<StockResponseDTO> response = stockService.obtenerTodosLosRegistrosDeStock();
	    return ResponseEntity.ok(response);
	}
	
	
	// Endpoint para obtener registros de stock por variedad
	@GetMapping("/obtener-variedad/{idVariedad}")
	@Operation(summary = "Obtener stock por variedad", description = "Lista los registros de stock asociados a una variedad.")
	public ResponseEntity<?> obetenerStockPorVariedad(
			@PathVariable @Parameter(description = "ID de la variedad", example = "2") Long idVariedad) {

	    List<StockResponseDTO> response = stockService.obtenerRegistrosDeStockPorVariedad(idVariedad);

	    // por seguridad chequeamos null o lista vacía
	    if (response == null || response.isEmpty()) {
	        String mensaje = "No hay stock registrado para la variedad con id " + idVariedad;
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensaje);
	    }

	    return ResponseEntity.ok(response);
	}
	
	
	// Endpoint para descontar stock de una variedad
	@PostMapping("/descontarStock/{idVariedad}/{cantidad}")
	@Operation(summary = "Descontar stock", description = "Descuenta manualmente una cantidad de la variedad indicada.")
	public ResponseEntity<String> descontarStock(
			@PathVariable @Parameter(description = "ID de la variedad", example = "2") Long idVariedad,
			@PathVariable @Parameter(description = "Cantidad a descontar", example = "6") Integer cantidad) {
	    try {
	    	
	        stockService.descontarStockVariedad(idVariedad, cantidad);
	        return ResponseEntity.ok("Stock descontado correctamente.");
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    }
	}
	
	
	
	// Endpoint para registar empanadas perdidas
	@PostMapping("/perdidas")
    @Operation(summary = "Registrar pérdidas", description = "Registra mermas y descuenta las unidades informadas.")
    public ResponseEntity<Void> registrarPerdidas(@RequestBody List<PerdidaEmpanadaDTO> perdidas) {
        stockService.registrarPerdidas(perdidas);
        return ResponseEntity.ok().build();
    }
	
	// Endpoint para resetar stock disponible
	@PostMapping("/ajustar")
	@Operation(summary = "Ajustar stock disponible", description = "Corrige manualmente la disponibilidad actual por variedad.")
	public ResponseEntity<Void> ajustarStockDisponible(
	        @RequestBody List<AjusteStockDTO> ajustes
	) {
	    stockService.ajustarStockDisponible(ajustes);
	    return ResponseEntity.ok().build();
	}
}
