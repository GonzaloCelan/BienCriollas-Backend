package com.bienCriollas.stock.Controller;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.data.domain.Pageable;


import com.bienCriollas.stock.Dto.EstadisticaDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.TipoEstado;
import com.bienCriollas.stock.Model.TipoVenta;
import com.bienCriollas.stock.Service.EstadisticaService;
import com.bienCriollas.stock.Service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/estadistica")
@RequiredArgsConstructor
public class EstadisticaController {

	private final EstadisticaService estadisticaService;
	private final PedidoService pedidoService;
		
	@GetMapping("/{fecha}")
	public ResponseEntity<EstadisticaDTO> obtenerEstadisticaPorFecha(@PathVariable 
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDate fecha) {
	    
		EstadisticaDTO response = estadisticaService.obtenerEstadisticasPorFecha(fecha);
		
		 return ResponseEntity.ok(response);
}
	
	@GetMapping("/mes/{año}/{mes}")
	public EstadisticaDTO obtenerEstadisticaPorMes(
	        @PathVariable int año,
	        @PathVariable int mes) {

	    return estadisticaService.obtenerEstadisticasPorMes(año, mes);
	}
	
	
	// 📊 ÚLTIMOS 7 DÍAS (incluye hoy)
    @GetMapping("/ultimos-7-dias")
    public ResponseEntity<EstadisticaDTO> obtenerEstadisticasUltimos7Dias() {

        EstadisticaDTO dto = estadisticaService.obtenerEstadisticasUltimos7Dias();

        return (dto != null)
                ? ResponseEntity.ok(dto)
                : ResponseEntity.noContent().build();
    }
	
	
	// ✅ DIA: /api/pedidos/entregados/dia?fecha=2025-12-22&page=0&size=20
	  @GetMapping("/entregados/dia")
	  public ResponseEntity<Page<Pedido>> entregadosDia(
	      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
	      @RequestParam(required = false) TipoVenta tipoVenta,
	      @RequestParam(defaultValue = "0") int page,
	      @RequestParam(defaultValue = "10") int size
	  ) {
	    Pageable pageable = PageRequest.of(page, size);
	    return ResponseEntity.ok(estadisticaService.listarEntregadosDelDia(fecha,tipoVenta, pageable));
	  }

	  
	  // ✅ MES: /api/pedidos/entregados/mes?anio=2025&mes=12&page=0&size=50
	  @GetMapping("/entregados/mes")
	  public ResponseEntity<Page<Pedido>> entregadosMes(
	      @RequestParam int anio,
	      @RequestParam int mes,
	      @RequestParam(required = false) TipoVenta tipoVenta,
	      @RequestParam(defaultValue = "0") int page,
	      @RequestParam(defaultValue = "10") int size
	  ) {
	    Pageable pageable = PageRequest.of(page, size);
	    return ResponseEntity.ok(estadisticaService.listarEntregadosDelMes(anio, mes,tipoVenta, pageable));
	  }
	  
	  
	  
	  @GetMapping("/pedidos")
	  
	    public ResponseEntity<Page<PedidoResponseDTO>> obtenerPedidosPorEstadoYFecha(
	            @RequestParam TipoEstado estado,

	            // opcional: si no viene, toma "hoy" (Argentina)
	            @RequestParam(required = false)
	            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	            LocalDate fecha,

	            @RequestParam(defaultValue = "0") int page,
	            @RequestParam(defaultValue = "50") int size
	    ) {
	        if (fecha == null) {
	            fecha = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
	        }

	        Page<PedidoResponseDTO> result =
	                pedidoService.obtenerPedidosPaginadosPorEstadoYFecha(estado, fecha, page, size);

	        return result.isEmpty()
	                ? ResponseEntity.noContent().build()
	                : ResponseEntity.ok(result);
	    }
}