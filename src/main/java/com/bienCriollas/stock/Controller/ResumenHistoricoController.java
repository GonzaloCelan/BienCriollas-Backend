package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.BalanceMensualDTO;
import com.bienCriollas.stock.Dto.ResumenAcumuladoDTO;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Service.PedidoService;
import com.bienCriollas.stock.Service.ResumenHistorialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resumen")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ResumenHistoricoController {

	private final ResumenHistorialService resumenService;
	
	
	@GetMapping("/acumulado")
	public ResponseEntity<ResumenAcumuladoDTO> obtenerAcumulado(
	        @RequestParam(required = false) Integer año,
	        @RequestParam(required = false) Integer mes
	) {
	    ResumenAcumuladoDTO dto = resumenService.obtenerAcumuladoHistorico(año, mes);

	    return (dto != null) ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
	}

	
	
	@GetMapping("/mensual/grafico")
    public ResponseEntity<List<BalanceMensualDTO>> resumenMensualGrafico(
            @RequestParam(value = "año", required = false) Integer anioConEnie,
            @RequestParam(value = "anio", required = false) Integer anioSinEnie
    ) {
        Integer anio = (anioConEnie != null) ? anioConEnie : anioSinEnie;

        List<BalanceMensualDTO> lista = resumenService.resumenMensualGrafico(anio);

        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(lista);
    }
	
	@GetMapping("/pedidosya/acumulado")
	public ResponseEntity<List<IngresoPedidosYa>> obtenerAcumuladoPedidosYa(
	) {
		List<IngresoPedidosYa> response = resumenService.obtenerPedidosYaLiquidaciones();

		return (response != null) ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
	}

}
