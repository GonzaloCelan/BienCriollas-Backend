package com.bienCriollas.stock.Controller;

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

import com.bienCriollas.stock.Dto.PerdidaEmpanadaDTO;
import com.bienCriollas.stock.Dto.StockDTO;
import com.bienCriollas.stock.Dto.StockResponseDTO;
import com.bienCriollas.stock.Interface.IStockService;


@RestController
@RequestMapping("/stock")
public class StockController {
	
	@Autowired
	private IStockService stockService;
	
	// Endpoint para actualizar stock en lote
	@PostMapping("/actualizar")
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
	public ResponseEntity<List<StockResponseDTO>> obetenerStockLote() {
		List<StockResponseDTO> response = stockService.obtenerTodosLosRegistrosDeStock();
	    return ResponseEntity.ok(response);
	}
	
	
	// Endpoint para obtener registros de stock por variedad
	@GetMapping("/obtener-variedad/{idVariedad}")
	public ResponseEntity<?> obetenerStockPorVariedad(@PathVariable Long idVariedad) {

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
	public ResponseEntity<String> descontarStock(@PathVariable Long idVariedad, @PathVariable Integer cantidad) {
	    try {
	    	
	        stockService.descontarStockVariedad(idVariedad, cantidad);
	        return ResponseEntity.ok("Stock descontado correctamente.");
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    }
	}
	
	
	
	// Endpoint para registar empanadas perdidas
	@PostMapping("/perdidas")
    public ResponseEntity<Void> registrarPerdidas(@RequestBody List<PerdidaEmpanadaDTO> perdidas) {
        stockService.registrarPerdidas(perdidas);
        return ResponseEntity.ok().build();
    }
}