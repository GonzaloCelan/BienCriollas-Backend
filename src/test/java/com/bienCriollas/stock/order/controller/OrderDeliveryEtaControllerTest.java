package com.bienCriollas.stock.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.bienCriollas.stock.order.dto.OrderEventDTO;
import com.bienCriollas.stock.order.dto.OrderResponseDTO;
import com.bienCriollas.stock.order.dto.UpdateDeliveryEtaRequestDTO;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.interfaces.IOrderService;

class OrderDeliveryEtaControllerTest {

    @Test
    void etaPatchPublishesRefreshEvent() {
        IOrderService service = mock(IOrderService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        OrderController controller = new OrderController(service, messaging);
        LocalDateTime eta = LocalDateTime.of(2026, 9, 28, 13, 51);
        OrderResponseDTO response = new OrderResponseDTO(
                2802L, "Carlos Castro", "PEDIDOS_YA", "TRANSFERENCIA", "PYA-2802", null,
                eta, new BigDecimal("25000"), true, OrderStatus.PREPARADO,
                null, null, null, false, false, true);
        when(service.updateDeliveryEta(2802L, 31)).thenReturn(response);

        ResponseEntity<OrderResponseDTO> result = controller.updateDeliveryEta(
                2802L, new UpdateDeliveryEtaRequestDTO(31));

        assertThat(result.getBody()).isEqualTo(response);
        verify(messaging).convertAndSend(
                "/topic/pedidos",
                new OrderEventDTO("ETA_DELIVERY_ACTUALIZADO", 2802L, "PREPARADO"));
    }
}
