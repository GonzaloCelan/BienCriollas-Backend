package com.bienCriollas.stock.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bienCriollas.stock.order.dto.*;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.enums.*;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.repository.*;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.stock.service.StockService;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@ExtendWith(MockitoExtension.class)
class OrderPaymentStatusServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private StockService stockService;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private EmpanadaVarietyRepository varietyRepository;
    @Mock private StockRepository stockRepository;
    @InjectMocks private OrderService orderService;

    @Test
    void creationUsesExplicitPaidStatusAndDefaultsOldRequestsToPendingPayment() {
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(1L).name("Carne").build();
        when(varietyRepository.findById(1L)).thenReturn(Optional.of(variety));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(order.isPagado() ? 1L : 2L);
            return order;
        });

        OrderResponseDTO paid = orderService.createOrder(request(true));
        OrderResponseDTO legacyWithoutField = orderService.createOrder(new OrderRequestDTO(
                "Cliente", "PARTICULAR", "EFECTIVO", null, LocalTime.of(20, 0),
                null, null, new BigDecimal("6000"),
                List.of(new OrderDetailRequestDTO(1L, 6))));

        assertThat(paid.orderStatus()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(paid.pagado()).isTrue();
        assertThat(legacyWithoutField.orderStatus()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(legacyWithoutField.pagado()).isFalse();
    }

    @Test
    void preparingPreservesPaidAndPendingPaymentStatuses() {
        Order paid = order(1L, OrderStatus.PENDIENTE, true);
        Order unpaid = order(2L, OrderStatus.PENDIENTE, false);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(paid));
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(unpaid));

        orderService.updateOrderStatus(1L, OrderStatus.PREPARADO);
        orderService.updateOrderStatus(2L, OrderStatus.PREPARADO);

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PREPARADO);
        assertThat(paid.isPagado()).isTrue();
        assertThat(unpaid.getStatus()).isEqualTo(OrderStatus.PREPARADO);
        assertThat(unpaid.isPagado()).isFalse();
    }

    @Test
    void deliveringAlwaysMarksOrderAsPaid() {
        Order alreadyPaid = order(1L, OrderStatus.PREPARADO, true);
        Order pendingPayment = order(2L, OrderStatus.PREPARADO, false);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(alreadyPaid));
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(pendingPayment));

        orderService.updateOrderStatus(1L, OrderStatus.ENTREGADO);
        orderService.updateOrderStatus(2L, OrderStatus.ENTREGADO);

        assertThat(alreadyPaid.isPagado()).isTrue();
        assertThat(pendingPayment.isPagado()).isTrue();
        assertThat(alreadyPaid.getStatus()).isEqualTo(OrderStatus.ENTREGADO);
        assertThat(pendingPayment.getStatus()).isEqualTo(OrderStatus.ENTREGADO);
    }

    @Test
    void quickPatchUpdatesPaymentButCannotUnpayDeliveredOrder() {
        Order prepared = order(1L, OrderStatus.PREPARADO, false);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(prepared));
        when(orderRepository.save(prepared)).thenReturn(prepared);

        OrderResponseDTO response = orderService.updatePaidStatus(1L, true);

        assertThat(response.pagado()).isTrue();
        assertThat(prepared.isPagado()).isTrue();

        Order delivered = order(2L, OrderStatus.ENTREGADO, true);
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> orderService.updatePaidStatus(2L, false))
                .isInstanceOf(OrderOperationNotAllowedException.class)
                .hasMessage("Un pedido entregado no puede quedar pendiente de pago.");
        verify(orderRepository, never()).save(delivered);
        assertThat(delivered.isPagado()).isTrue();
    }

    @Test
    void cancellationPreservesPaymentStatus() {
        Order paid = order(1L, OrderStatus.PREPARADO, true);
        Order unpaid = order(2L, OrderStatus.PREPARADO, false);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(paid));
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(unpaid));

        orderService.updateOrderStatus(1L, OrderStatus.CANCELADO);
        orderService.updateOrderStatus(2L, OrderStatus.CANCELADO);

        assertThat(paid.isPagado()).isTrue();
        assertThat(unpaid.isPagado()).isFalse();
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(unpaid.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    void fullUpdateCanCorrectPaymentBeforeDeliveryAndDetailExposesIt() {
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(1L).name("Carne").build();
        Order existing = order(1L, OrderStatus.PREPARADO, false);
        existing.getDetails().add(com.bienCriollas.stock.order.entity.OrderDetail.builder()
                .order(existing).variety(variety).quantity(6).build());
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(varietyRepository.findById(1L)).thenReturn(Optional.of(variety));
        when(orderRepository.save(existing)).thenReturn(existing);

        OrderResponseDTO updated = orderService.updateOrder(1L, request(true));

        assertThat(updated.pagado()).isTrue();
        OrderRequestDTO legacyUpdate = new OrderRequestDTO(
                "Cliente", "PARTICULAR", "EFECTIVO", null, LocalTime.of(20, 0),
                null, null, new BigDecimal("6000"),
                List.of(new OrderDetailRequestDTO(1L, 6)));
        assertThat(orderService.updateOrder(1L, legacyUpdate).pagado()).isTrue();
        assertThat(orderService.getOrderDetails(1L))
                .extracting(OrderDetailResponseDTO::pagado)
                .containsOnly(true);
    }

    private OrderRequestDTO request(boolean pagado) {
        return new OrderRequestDTO(
                "Cliente", "PARTICULAR", "EFECTIVO", null, LocalTime.of(20, 0),
                null, null, new BigDecimal("6000"),
                List.of(new OrderDetailRequestDTO(1L, 6)), null, pagado);
    }

    private Order order(Long id, OrderStatus status, boolean pagado) {
        return Order.builder()
                .orderId(id)
                .customer("Cliente")
                .saleType(SaleType.PARTICULAR)
                .paymentType(PaymentType.EFECTIVO)
                .cashAmount(new BigDecimal("6000"))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal("6000"))
                .status(status)
                .pagado(pagado)
                .stockDiscounted(true)
                .details(new ArrayList<>())
                .build();
    }
}
