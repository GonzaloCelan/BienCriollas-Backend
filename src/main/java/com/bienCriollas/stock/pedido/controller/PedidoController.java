package com.bienCriollas.stock.pedido.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.pedido.dto.PedidoDetalleResponseDTO;
import com.bienCriollas.stock.pedido.dto.PedidoEventoDTO;
import com.bienCriollas.stock.pedido.dto.PedidoRequestDTO;
import com.bienCriollas.stock.pedido.dto.PedidoResponseDTO;
import com.bienCriollas.stock.pedido.interfaces.IPedidoService;
import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.enums.TipoPago;

@RestController
@RequestMapping("api/v2/pedido")
public class PedidoController {

	
	@Autowired
	private IPedidoService pedidoService;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	
	@PostMapping("/crear")
	public ResponseEntity<PedidoResponseDTO> crearPedido(@RequestBody PedidoRequestDTO pedido) {
	    PedidoResponseDTO response = pedidoService.crearPedido(pedido);

	    messagingTemplate.convertAndSend(
	            "/topic/pedidos",
	            new PedidoEventoDTO(
	                    "CREADO",
	                    response.idPedido(),
	                    response.estadoPedido().name()
	            )
	    );

	    return ResponseEntity.ok(response);
	}
	
	
	@PutMapping("/actualizar-estado/{id}/{nuevoEstado}")
	public ResponseEntity<Boolean> actualizarEstadoPedido(
	        @PathVariable Long id,
	        @PathVariable String nuevoEstado) {

	    TipoEstado estadoEnum;

	    try {
	        estadoEnum = TipoEstado.valueOf(nuevoEstado.toUpperCase());
	    } catch (IllegalArgumentException e) {
	        throw new RuntimeException("Estado inválido: " + nuevoEstado);
	    }

	    Boolean response = pedidoService.actualizarEstadoPedido(id, estadoEnum);

	    if (Boolean.TRUE.equals(response)) {
	        String tipoEvento = estadoEnum.name().equals("CANCELADO")
	                ? "CANCELADO"
	                : "ACTUALIZADO";

	        messagingTemplate.convertAndSend(
	                "/topic/pedidos",
	                new PedidoEventoDTO(
	                        tipoEvento,
	                        id,
	                        estadoEnum.name()
	                )
	        );
	    }

	    return ResponseEntity.ok(response);
	}
	
	
	
	
	
	@PutMapping("/actualizar-pago/{id}/{nuevoPago}")
	public ResponseEntity<Boolean> actualizarTipoPago(
	        @PathVariable Long id,
	        @PathVariable String nuevoPago) {

	    TipoPago pagoEnum;
	    try {
	    	pagoEnum = TipoPago.valueOf(nuevoPago.toUpperCase());
	    } catch (IllegalArgumentException e) {
	        throw new RuntimeException("Estado inválido: " + nuevoPago);
	    }

	    Boolean response = pedidoService.actualizarTipoPago(id, pagoEnum);
	    return ResponseEntity.ok(response);
	}
	
	@GetMapping("/pedido-estado/{estado}")
	public ResponseEntity<?> obtenerPedidosPorEstado(
	        @PathVariable String estado,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size
	) {
	    TipoEstado estadoEnum;
	    try {
	        estadoEnum = TipoEstado.valueOf(estado.toUpperCase());
	    } catch (IllegalArgumentException e) {
	        throw new RuntimeException("Estado inválido: " + estado);
	    }

	    return ResponseEntity.ok(pedidoService.obtenerPedidosPaginados(estadoEnum, page, size));
	}

	
	
	@GetMapping("/por-fecha/{fecha}")
	public ResponseEntity<?> obtenerPedidosPorFecha( @PathVariable
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	        LocalDate fecha) {
		
	    return ResponseEntity.ok(pedidoService.obtenerPedidosPorFecha(fecha));
	}
	
	
	@GetMapping("/detalle/{id}")
	public ResponseEntity<List<PedidoDetalleResponseDTO>> obtenerDetallePedido(@PathVariable Long id) {
		List<PedidoDetalleResponseDTO> response = pedidoService.obtenerDetallesPedido(id);
	    
		return ResponseEntity.ok(response);
	}
	
	
	@GetMapping("/paginado")
	public ResponseEntity<Page<PedidoResponseDTO>> obtenerPedidosPaginados(
	        @RequestParam TipoEstado estado,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size
	) {
	    Page<PedidoResponseDTO> result = pedidoService.obtenerPedidosPaginados(estado, page, size);
	    return ResponseEntity.ok(result);
	}

	
}
