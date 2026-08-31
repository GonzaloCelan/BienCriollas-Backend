package com.bienCriollas.stock.pedido.controller;

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

import com.bienCriollas.stock.pedido.dto.PedidoEventoDTO;
import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoConsultaDTO;
import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoResultadoDTO;
import com.bienCriollas.stock.pedido.dto.RegularizarPedidosRequestDTO;
import com.bienCriollas.stock.pedido.service.RegularizacionPedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/pedido/regularizacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Regularización de pedidos", description = "Corrección administrativa y auditada de pedidos sin cerrar.")
@RequiredArgsConstructor
public class RegularizacionPedidoController {

    private final RegularizacionPedidoService regularizacionPedidoService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    @Operation(summary = "Consultar pedidos sin cerrar de un mes")
    public ResponseEntity<RegularizacionPedidoConsultaDTO> consultarMes(
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
                regularizacionPedidoService.consultarMes(anio, mes, page, size));
    }

    @PatchMapping("/entregar")
    @Operation(summary = "Marcar como entregados los pedidos seleccionados")
    public ResponseEntity<RegularizacionPedidoResultadoDTO> marcarComoEntregados(
            @Valid @RequestBody RegularizarPedidosRequestDTO request,
            Authentication authentication) {
        RegularizacionPedidoResultadoDTO resultado = regularizacionPedidoService.regularizar(
                request,
                authentication.getName());

        for (Long idPedido : resultado.idsPedidos()) {
            messagingTemplate.convertAndSend(
                    "/topic/pedidos",
                    new PedidoEventoDTO("REGULARIZADO", idPedido, "ENTREGADO"));
        }
        return ResponseEntity.ok(resultado);
    }
}
