package com.bienCriollas.stock.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.order.dto.OrderReconciliationQueryDTO;
import com.bienCriollas.stock.order.dto.OrderReconciliationResultDTO;
import com.bienCriollas.stock.order.dto.ReconcileOrdersRequestDTO;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.entity.OrderReconciliation;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.order.repository.OrderReconciliationRepository;

@ExtendWith(MockitoExtension.class)
class OrderReconciliationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderReconciliationRepository orderReconciliationRepository;

    @InjectMocks
    private OrderReconciliationService service;

    @Test
    void queryMonthReturnsPendingPreparedAndPotentialTotal() {
        Order pending = order(1L, OrderStatus.PENDIENTE, "3500.00");
        Order prepared = order(2L, OrderStatus.PREPARADO, "6500.00");
        when(orderRepository
                .findByStatusInAndCreationDateGreaterThanEqualAndCreationDateLessThan(
                        any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending, prepared)));
        when(orderRepository.sumTotalByStatusesAndPeriod(any(), any(), any()))
                .thenReturn(new BigDecimal("10000.00"));

        OrderReconciliationQueryDTO result = service.queryMonth(2026, 8, 0, 50);

        assertEquals(2, result.orderCount());
        assertEquals(new BigDecimal("10000.00"), result.potentialIncome());
        assertEquals(List.of(1L, 2L), result.orders().stream()
                .map(item -> item.orderId())
                .toList());
    }

    @Test
    void reconcileUpdatesEntireBatchAndRecordsAudit() {
        Order pending = order(1L, OrderStatus.PENDIENTE, "3500.00");
        Order prepared = order(2L, OrderStatus.PREPARADO, "6500.00");
        when(orderRepository.findAllByIdForReconciliation(any()))
                .thenReturn(List.of(pending, prepared));
        ReconcileOrdersRequestDTO request = new ReconcileOrdersRequestDTO(
                2026,
                8,
                List.of(1L, 2L),
                "Cierre de agosto",
                true);

        OrderReconciliationResultDTO result = service.reconcile(request, "admin");

        assertEquals(OrderStatus.ENTREGADO, pending.getStatus());
        assertEquals(OrderStatus.ENTREGADO, prepared.getStatus());
        assertEquals(2, result.updatedCount());
        assertEquals(new BigDecimal("10000.00"), result.incorporatedIncome());
        assertEquals("admin", result.performedBy());
        assertEquals(List.of(1L, 2L), result.orderIds());
        verify(orderRepository).saveAll(List.of(pending, prepared));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderReconciliation>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderReconciliationRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(OrderStatus.PENDIENTE, captor.getValue().get(0).getPreviousStatus());
        assertEquals(OrderStatus.PREPARADO, captor.getValue().get(1).getPreviousStatus());
        assertEquals(result.batchId(), captor.getValue().get(0).getBatchId());
        assertEquals(result.batchId(), captor.getValue().get(1).getBatchId());
    }

    @Test
    void reconcileRejectsEntireBatchWhenAnOrderIsNoLongerEligible() {
        Order pending = order(1L, OrderStatus.PENDIENTE, "3500.00");
        Order cancelled = order(2L, OrderStatus.CANCELADO, "6500.00");
        when(orderRepository.findAllByIdForReconciliation(any()))
                .thenReturn(List.of(pending, cancelled));
        ReconcileOrdersRequestDTO request = new ReconcileOrdersRequestDTO(
                2026,
                8,
                List.of(1L, 2L),
                "Cierre de agosto",
                true);

        OrderOperationNotAllowedException error = assertThrows(
                OrderOperationNotAllowedException.class,
                () -> service.reconcile(request, "admin"));

        assertEquals("El pedido 2 ya no está PENDIENTE o PREPARADO", error.getMessage());
        assertEquals(OrderStatus.PENDIENTE, pending.getStatus());
        verify(orderRepository, never()).saveAll(any());
        verify(orderReconciliationRepository, never()).saveAll(any());
    }

    private Order order(Long id, OrderStatus status, String total) {
        return Order.builder()
                .orderId(id)
                .creationDate(LocalDate.of(2026, 8, 15))
                .customer("Cliente " + id)
                .saleType(SaleType.PARTICULAR)
                .paymentType(PaymentType.EFECTIVO)
                .cashAmount(new BigDecimal(total))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal(total))
                .status(status)
                .build();
    }
}
