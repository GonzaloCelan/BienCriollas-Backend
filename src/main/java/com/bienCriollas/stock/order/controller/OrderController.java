package com.bienCriollas.stock.order.controller;

import java.time.LocalDate;
import java.util.List;

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

import com.bienCriollas.stock.order.dto.OrderDetailResponseDTO;
import com.bienCriollas.stock.order.dto.OrderEventDTO;
import com.bienCriollas.stock.order.dto.OrderRequestDTO;
import com.bienCriollas.stock.order.dto.OrderResponseDTO;
import com.bienCriollas.stock.order.dto.UpdatePaymentRequestDTO;
import com.bienCriollas.stock.order.interfaces.IOrderService;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.exception.InvalidOrderException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v2/pedido")
@Tag(name = "Pedidos", description = "Operaciones del ciclo completo de pedidos.")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;


    @PostMapping("/crear")
    @Operation(summary = "Crear un pedido", description = "Registra el pedido, descuenta el stock y publica un evento WebSocket.")
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO order) {
        OrderResponseDTO response = orderService.createOrder(order);

        messagingTemplate.convertAndSend(
                "/topic/pedidos",
                new OrderEventDTO(
                        "CREADO",
                        response.orderId(),
                        response.orderStatus().name()
                )
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/actualizar/{id}")
    @Operation(summary = "Actualizar un pedido completo", description = "Reemplaza sus datos y detalles, devolviendo el stock anterior y descontando el nuevo.")
    public ResponseEntity<OrderResponseDTO> updateOrder(
            @PathVariable @Parameter(description = "ID del pedido", example = "123") Long id,
            @RequestBody OrderRequestDTO order) {

        OrderResponseDTO response = orderService.updateOrder(id, order);

        messagingTemplate.convertAndSend(
                "/topic/pedidos",
                new OrderEventDTO(
                        "ACTUALIZADO",
                        response.orderId(),
                        response.orderStatus().name()
                )
        );

        return ResponseEntity.ok(response);
    }


    @PutMapping("/actualizar-estado/{id}/{nuevoEstado}")
    @Operation(summary = "Cambiar el estado de un pedido", description = "Actualiza el estado y notifica el cambio en /topic/pedidos.")
    public ResponseEntity<Boolean> updateOrderStatus(
            @PathVariable @Parameter(description = "ID del pedido", example = "123") Long id,
            @PathVariable("nuevoEstado") @Parameter(description = "PENDIENTE, PREPARADO, ENTREGADO o CANCELADO", example = "PREPARADO") String newStatus) {

        OrderStatus statusEnum;

        try {
            statusEnum = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("Estado inválido: " + newStatus);
        }

        Boolean response = orderService.updateOrderStatus(id, statusEnum);

        if (Boolean.TRUE.equals(response)) {
            String eventType = statusEnum.name().equals("CANCELADO")
                    ? "CANCELADO"
                    : "ACTUALIZADO";

            messagingTemplate.convertAndSend(
                    "/topic/pedidos",
                    new OrderEventDTO(
                            eventType,
                            id,
                            statusEnum.name()
                    )
            );
        }

        return ResponseEntity.ok(response);
    }





    @PutMapping("/actualizar-pago/{id}/{nuevoPago}")
    @Operation(
            summary = "Cambiar el medio de pago",
            description = "Actualiza el tipo de pago. Para COMBINADO requiere montoEfectivo y montoTransferencia en el body.")
    public ResponseEntity<Boolean> updatePaymentType(
            @PathVariable @Parameter(description = "ID del pedido", example = "123") Long id,
            @PathVariable("nuevoPago") @Parameter(description = "EFECTIVO, TRANSFERENCIA o COMBINADO", example = "EFECTIVO") String newPayment,
            @RequestBody(required = false) UpdatePaymentRequestDTO amounts) {

        PaymentType paymentTypeEnum;
        try {
             paymentTypeEnum = PaymentType.valueOf(newPayment.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("Tipo de pago inválido: " + newPayment);
        }

        Boolean response = orderService.updatePaymentType(
                id,
                paymentTypeEnum,
                amounts == null ? null : amounts.cashAmount(),
                amounts == null ? null : amounts.transferAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pedido-estado/{estado}")
    @Operation(summary = "Listar pedidos por estado", description = "Devuelve los pedidos del día paginados y ordenados para la operación.")
    public ResponseEntity<?> getOrdersByStatus(
            @PathVariable("estado") @Parameter(description = "Estado del pedido", example = "PENDIENTE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        OrderStatus statusEnum;
        try {
            statusEnum = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("Estado inválido: " + status);
        }

        return ResponseEntity.ok(orderService.getPagedOrders(statusEnum, page, size));
    }



    @GetMapping("/por-fecha/{fecha}")
    @Operation(summary = "Listar pedidos por fecha", description = "Busca todos los pedidos de una fecha determinada.")
    public ResponseEntity<?> getOrdersByDate( @PathVariable("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(orderService.getOrdersByDate(date));
    }


    @GetMapping("/detalle/{id}")
    @Operation(summary = "Obtener el detalle de un pedido", description = "Devuelve variedades, cantidades y subtotales del pedido.")
    public ResponseEntity<List<OrderDetailResponseDTO>> getOrderDetail(@PathVariable Long id) {
        List<OrderDetailResponseDTO> response = orderService.getOrderDetails(id);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/paginado")
    @Operation(summary = "Listar pedidos paginados", description = "Consulta pedidos por estado utilizando paginación explícita.")
    public ResponseEntity<Page<OrderResponseDTO>> getPagedOrders(
            @RequestParam("estado") OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrderResponseDTO> result = orderService.getPagedOrders(status, page, size);
        return ResponseEntity.ok(result);
    }


}
