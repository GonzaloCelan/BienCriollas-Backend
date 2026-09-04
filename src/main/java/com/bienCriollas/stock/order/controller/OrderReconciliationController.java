package com.bienCriollas.stock.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.order.dto.OrderEventDTO;
import com.bienCriollas.stock.order.dto.OrderReconciliationQueryDTO;
import com.bienCriollas.stock.order.dto.OrderReconciliationResultDTO;
import com.bienCriollas.stock.order.dto.ReconcileOrdersRequestDTO;
import com.bienCriollas.stock.order.service.OrderReconciliationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/pedido/regularizacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Regularización de pedidos", description = "Corrección administrativa y auditada de pedidos sin cerrar.")
@RequiredArgsConstructor
public class OrderReconciliationController {

    private final OrderReconciliationService orderReconciliationService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    @Operation(summary = "Consultar pedidos sin cerrar de un mes")
    public ResponseEntity<OrderReconciliationQueryDTO> queryMonth(
            @RequestParam("anio") Integer year,
            @RequestParam("mes") Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
                orderReconciliationService.queryMonth(year, month, page, size));
    }

    @PatchMapping("/entregar")
    @Operation(summary = "Marcar como entregados los pedidos seleccionados")
    public ResponseEntity<OrderReconciliationResultDTO> markAsDelivered(
            @Valid @RequestBody ReconcileOrdersRequestDTO request,
            Authentication authentication) {
        OrderReconciliationResultDTO result = orderReconciliationService.reconcile(
                request,
                authentication.getName());

        for (Long orderId : result.orderIds()) {
            messagingTemplate.convertAndSend(
                    "/topic/pedidos",
                    new OrderEventDTO("REGULARIZADO", orderId, "ENTREGADO"));
        }
        return ResponseEntity.ok(result);
    }
}
