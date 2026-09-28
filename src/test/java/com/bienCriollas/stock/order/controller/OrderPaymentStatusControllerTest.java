package com.bienCriollas.stock.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.bienCriollas.stock.order.dto.*;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.interfaces.IOrderService;

class OrderPaymentStatusControllerTest {

    @Test
    void quickPaymentUpdatePublishesOrderRefreshEvent() {
        IOrderService service = mock(IOrderService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        OrderController controller = new OrderController(service, messaging);
        OrderResponseDTO response = new OrderResponseDTO(
                25L, "Juan Pérez", "PARTICULAR", "EFECTIVO", null, null,
                null, new BigDecimal("25000"), true, OrderStatus.PREPARADO,
                null, null, null, false, false, true);
        when(service.updatePaidStatus(25L, true)).thenReturn(response);

        ResponseEntity<OrderResponseDTO> result = controller.updatePaidStatus(
                25L, new UpdatePaidStatusRequestDTO(true));

        assertThat(result.getBody()).isEqualTo(response);
        verify(messaging).convertAndSend(
                "/topic/pedidos",
                new OrderEventDTO("PAGO_ACTUALIZADO", 25L, "PREPARADO"));
    }
}
