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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/v2/pedido")
@Tag(name = "Pedidos", description = "Operaciones del ciclo completo de pedidos.")
public class PedidoController {

	
	@Autowired
	private IPedidoService pedidoService;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	
	@PostMapping("/crear")
	@Operation(summary = "Crear un pedido", description = "Registra el pedido, descuenta el stock y publica un evento WebSocket.")
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

	@PutMapping("/actualizar/{id}")
	@Operation(summary = "Actualizar un pedido completo", description = "Reemplaza sus datos y detalles, devolviendo el stock anterior y descontando el nuevo.")
	public ResponseEntity<PedidoResponseDTO> actualizarPedido(
			@PathVariable @Parameter(description = "ID del pedido", example = "123") Long id,
			@RequestBody PedidoRequestDTO pedido) {

		PedidoResponseDTO response = pedidoService.actualizarPedido(id, pedido);

		messagingTemplate.convertAndSend(
				"/topic/pedidos",
				new PedidoEventoDTO(
						"ACTUALIZADO",
						response.idPedido(),
						response.estadoPedido().name()
				)
		);

		return ResponseEntity.ok(response);
	}
	
	
	@PutMapping("/actualizar-estado/{id}/{nuevoEstado}")
	@Operation(summary = "Cambiar el estado de un pedido", description = "Actualiza el estado y notifica el cambio en /topic/pedidos.")
	public ResponseEntity<Boolean> actualizarEstadoPedido(
	        @PathVariable @Parameter(description = "ID del pedido", example = "123") Long id,
	        @PathVariable @Parameter(description = "PENDIENTE, PREPARADO, ENTREGADO o CANCELADO", example = "PREPARADO") String nuevoEstado) {

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
	@Operation(summary = "Cambiar el medio de pago", description = "Actualiza el tipo de pago asociado al pedido.")
	public ResponseEntity<Boolean> actualizarTipoPago(
	        @PathVariable @Parameter(description = "ID del pedido", example = "123") Long id,
	        @PathVariable @Parameter(description = "EFECTIVO, TRANSFERENCIA o COMBINADO", example = "EFECTIVO") String nuevoPago) {

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
	@Operation(summary = "Listar pedidos por estado", description = "Devuelve los pedidos del día paginados y ordenados para la operación.")
	public ResponseEntity<?> obtenerPedidosPorEstado(
	        @PathVariable @Parameter(description = "Estado del pedido", example = "PENDIENTE") String estado,
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
	@Operation(summary = "Listar pedidos por fecha", description = "Busca todos los pedidos de una fecha determinada.")
	public ResponseEntity<?> obtenerPedidosPorFecha( @PathVariable
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	        LocalDate fecha) {
		
	    return ResponseEntity.ok(pedidoService.obtenerPedidosPorFecha(fecha));
	}
	
	
	@GetMapping("/detalle/{id}")
	@Operation(summary = "Obtener el detalle de un pedido", description = "Devuelve variedades, cantidades y subtotales del pedido.")
	public ResponseEntity<List<PedidoDetalleResponseDTO>> obtenerDetallePedido(@PathVariable Long id) {
		List<PedidoDetalleResponseDTO> response = pedidoService.obtenerDetallesPedido(id);
	    
		return ResponseEntity.ok(response);
	}
	
	
	@GetMapping("/paginado")
	@Operation(summary = "Listar pedidos paginados", description = "Consulta pedidos por estado utilizando paginación explícita.")
	public ResponseEntity<Page<PedidoResponseDTO>> obtenerPedidosPaginados(
	        @RequestParam TipoEstado estado,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size
	) {
	    Page<PedidoResponseDTO> result = pedidoService.obtenerPedidosPaginados(estado, page, size);
	    return ResponseEntity.ok(result);
	}

	
}
