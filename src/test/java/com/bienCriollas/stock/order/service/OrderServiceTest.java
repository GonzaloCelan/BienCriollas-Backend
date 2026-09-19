package com.bienCriollas.stock.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bienCriollas.stock.order.dto.OrderDetailRequestDTO;
import com.bienCriollas.stock.order.dto.OrderRequestDTO;
import com.bienCriollas.stock.order.dto.OrderResponseDTO;
import com.bienCriollas.stock.order.dto.ScheduledOrderSummaryDTO;
import com.bienCriollas.stock.order.entity.OrderDetail;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.exception.OrderNotFoundException;
import com.bienCriollas.stock.order.exception.InvalidOrderException;
import com.bienCriollas.stock.order.repository.OrderDetailRepository;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.stock.service.StockService;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockService stockService;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private EmpanadaVarietyRepository empanadaVarietyRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderSavesAndReturnsDeliveryTime() {
        LocalTime deliveryTime = LocalTime.of(21, 30);
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(2L)
                .name("Pollo")
                .build();
        OrderRequestDTO request = new OrderRequestDTO(
                "   JULIETA    VARGAS ",
                "PARTICULAR",
                "EFECTIVO",
                null,
                deliveryTime,
                null,
                null,
                new BigDecimal("9000"),
                List.of(new OrderDetailRequestDTO(2L, 6)));

        when(empanadaVarietyRepository.findById(2L)).thenReturn(Optional.of(variety));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(10L);
            return order;
        });

        OrderResponseDTO response = orderService.createOrder(request);

        assertEquals(deliveryTime, response.deliveryTime());
        assertEquals("Julieta Vargas", response.customer());
        verify(orderRepository).save(any(Order.class));
        verify(stockService).adjustAvailability(eq(Map.of(2L, -6)));
    }

    @Test
    void createFutureOrderKeepsPhysicalStockAndMarksItAsScheduled() {
        LocalDate deliveryDate = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).plusDays(3);
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(1L)
                .name("Carne")
                .build();
        OrderRequestDTO request = new OrderRequestDTO(
                "Juan Perez",
                "PARTICULAR",
                "EFECTIVO",
                null,
                LocalTime.of(21, 0),
                null,
                null,
                new BigDecimal("18000"),
                List.of(new OrderDetailRequestDTO(1L, 12)),
                deliveryDate);

        when(empanadaVarietyRepository.findById(1L)).thenReturn(Optional.of(variety));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(500L);
            return order;
        });

        OrderResponseDTO response = orderService.createOrder(request);

        assertEquals(deliveryDate, response.deliveryDate());
        assertEquals(true, response.scheduled());
        assertEquals(false, response.deliveryToday());
        assertEquals(false, response.stockDiscounted());
        verify(stockService, never()).adjustAvailability(any());
    }

    @Test
    void preparingAndThenDeliveringScheduledOrderDiscountsStockOnlyOnce() {
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(1L)
                .name("Carne")
                .build();
        Order order = Order.builder()
                .orderId(500L)
                .status(OrderStatus.PENDIENTE)
                .stockDiscounted(false)
                .details(new ArrayList<>())
                .build();
        order.getDetails().add(OrderDetail.builder()
                .order(order)
                .variety(variety)
                .quantity(12)
                .build());
        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(500L, OrderStatus.PREPARADO);
        orderService.updateOrderStatus(500L, OrderStatus.ENTREGADO);

        assertEquals(true, order.getStockDiscounted());
        assertEquals(OrderStatus.ENTREGADO, order.getStatus());
        verify(stockService, times(1)).adjustAvailability(eq(Map.of(1L, -12)));
        verify(orderRepository, times(2)).save(order);
    }

    @Test
    void deliveringPreparedScheduledOrderDiscountsStockWhenItWasStillPending() {
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(1L)
                .name("Carne")
                .build();
        Order order = Order.builder()
                .orderId(501L)
                .status(OrderStatus.PREPARADO)
                .stockDiscounted(false)
                .details(new ArrayList<>())
                .build();
        order.getDetails().add(OrderDetail.builder()
                .order(order)
                .variety(variety)
                .quantity(12)
                .build());
        when(orderRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(501L, OrderStatus.ENTREGADO);

        assertEquals(OrderStatus.ENTREGADO, order.getStatus());
        assertEquals(true, order.getStockDiscounted());
        verify(stockService, times(1)).adjustAvailability(eq(Map.of(1L, -12)));
        verify(orderRepository).save(order);
    }

    @Test
    void cancellingUndiscountedScheduledOrderDoesNotReturnStock() {
        Order order = Order.builder()
                .orderId(500L)
                .status(OrderStatus.PENDIENTE)
                .stockDiscounted(false)
                .details(new ArrayList<>())
                .build();
        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(500L, OrderStatus.CANCELADO);

        assertEquals(OrderStatus.CANCELADO, order.getStatus());
        assertEquals(false, order.getStockDiscounted());
        verify(stockService, never()).adjustAvailability(any());
    }

    @Test
    void cancellingDiscountedPendingOrderReturnsItsStock() {
        EmpanadaVariety variety = EmpanadaVariety.builder().varietyId(1L).name("Carne").build();
        Order order = orderWithStock(502L, OrderStatus.PENDIENTE, true, null, variety, 12);
        when(orderRepository.findByIdForUpdate(502L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(502L, OrderStatus.CANCELADO);

        assertEquals(OrderStatus.CANCELADO, order.getStatus());
        assertEquals(false, order.getStockDiscounted());
        verify(stockService).adjustAvailability(eq(Map.of(1L, 12)));
        verify(orderRepository).save(order);
    }

    @Test
    void cancellingPreparedNormalAndScheduledOrdersKeepsTheirStockDiscounted() {
        EmpanadaVariety variety = EmpanadaVariety.builder().varietyId(1L).name("Carne").build();
        Order normal = orderWithStock(503L, OrderStatus.PREPARADO, true, null, variety, 12);
        Order scheduled = orderWithStock(504L, OrderStatus.PREPARADO, true,
                LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).plusDays(1),
                variety, 24);
        when(orderRepository.findByIdForUpdate(503L)).thenReturn(Optional.of(normal));
        when(orderRepository.findByIdForUpdate(504L)).thenReturn(Optional.of(scheduled));

        orderService.updateOrderStatus(503L, OrderStatus.CANCELADO);
        orderService.updateOrderStatus(504L, OrderStatus.CANCELADO);

        assertEquals(OrderStatus.CANCELADO, normal.getStatus());
        assertEquals(OrderStatus.CANCELADO, scheduled.getStatus());
        assertEquals(true, normal.getStockDiscounted());
        assertEquals(true, scheduled.getStockDiscounted());
        verify(stockService, never()).adjustAvailability(any());
        verify(orderRepository).save(normal);
        verify(orderRepository).save(scheduled);
    }

    private Order orderWithStock(Long id, OrderStatus status, boolean stockDiscounted,
            LocalDate deliveryDate, EmpanadaVariety variety, int quantity) {
        Order order = Order.builder()
                .orderId(id)
                .status(status)
                .saleType(SaleType.PARTICULAR)
                .stockDiscounted(stockDiscounted)
                .deliveryDate(deliveryDate)
                .cashAmount(new BigDecimal("1000"))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal("1000"))
                .details(new ArrayList<>())
                .build();
        order.getDetails().add(OrderDetail.builder()
                .order(order)
                .variety(variety)
                .quantity(quantity)
                .build());
        return order;
    }

    @Test
    void createOrderRejectsPastDeliveryDate() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).minusDays(1);
        OrderRequestDTO request = new OrderRequestDTO(
                "Juan Perez",
                "PARTICULAR",
                "EFECTIVO",
                null,
                LocalTime.of(21, 0),
                null,
                null,
                new BigDecimal("18000"),
                List.of(new OrderDetailRequestDTO(1L, 12)),
                yesterday);

        assertThrows(InvalidOrderException.class, () -> orderService.createOrder(request));

        verify(orderRepository, never()).save(any(Order.class));
        verify(stockService, never()).adjustAvailability(any());
    }

    @Test
    void scheduledSummaryExcludesDeliveredOrders() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        Order pending = Order.builder().status(OrderStatus.PENDIENTE).build();
        Order prepared = Order.builder().status(OrderStatus.PREPARADO).build();
        Order delivered = Order.builder().status(OrderStatus.ENTREGADO).build();

        when(orderRepository.findScheduledOrdersOn(today, OrderStatus.CANCELADO))
                .thenReturn(List.of(pending, delivered));
        when(orderRepository.findScheduledOrdersAfter(today, OrderStatus.CANCELADO))
                .thenReturn(List.of(pending, prepared, delivered));
        when(orderRepository.findScheduledOrdersOn(today.plusDays(1), OrderStatus.CANCELADO))
                .thenReturn(List.of(prepared, delivered));
        when(orderDetailRepository.sumCommittedStockByVariety(any(), any()))
                .thenReturn(List.of());

        ScheduledOrderSummaryDTO summary = orderService.getScheduledSummary();

        assertEquals(3, summary.totalScheduledOrders());
        assertEquals(1, summary.ordersForToday());
        assertEquals(1, summary.ordersForTomorrow());
        assertEquals(0, summary.totalCommittedUnits());
    }

    @Test
    void updateOrderReplacesDataDetailsAndStock() {
        EmpanadaVariety previousVariety = EmpanadaVariety.builder()
                .varietyId(1L)
                .name("Carne")
                .build();
        EmpanadaVariety newVariety = EmpanadaVariety.builder()
                .varietyId(2L)
                .name("Pollo")
                .build();

        Order order = Order.builder()
                .orderId(10L)
                .customer("Cliente anterior")
                .saleType(SaleType.PARTICULAR)
                .paymentType(PaymentType.EFECTIVO)
                .cashAmount(new BigDecimal("5000"))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal("5000"))
                .status(OrderStatus.PENDIENTE)
                .details(new ArrayList<>())
                .build();
        order.getDetails().add(OrderDetail.builder()
                .order(order)
                .variety(previousVariety)
                .quantity(4)
                .build());

        OrderRequestDTO request = new OrderRequestDTO(
                "mArCeLo JAIME",
                "particular",
                "transferencia",
                null,
                LocalTime.of(21, 30),
                null,
                null,
                new BigDecimal("9000"),
                List.of(new OrderDetailRequestDTO(2L, 6)));

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(empanadaVarietyRepository.findById(2L)).thenReturn(Optional.of(newVariety));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.updateOrder(10L, request);

        assertEquals("Marcelo Jaime", order.getCustomer());
        assertEquals(SaleType.PARTICULAR, order.getSaleType());
        assertEquals(PaymentType.TRANSFERENCIA, order.getPaymentType());
        assertEquals(BigDecimal.ZERO, order.getCashAmount());
        assertEquals(new BigDecimal("9000"), order.getTransferAmount());
        assertEquals(new BigDecimal("9000"), order.getOrderTotal());
        assertEquals(LocalTime.of(21, 30), order.getDeliveryTime());
        assertEquals(1, order.getDetails().size());
        assertEquals(2L, order.getDetails().get(0).getVariety().getVarietyId());
        assertEquals(6, order.getDetails().get(0).getQuantity());
        assertEquals(10L, response.orderId());

        verify(stockService).adjustAvailability(eq(Map.of(1L, 4, 2L, -6)));
    }

    @Test
    void updateOrderRejectsDeliveredOrders() {
        Order order = Order.builder()
                .orderId(10L)
                .status(OrderStatus.ENTREGADO)
                .details(new ArrayList<>())
                .build();
        OrderRequestDTO request = new OrderRequestDTO(
                "Cliente",
                "PARTICULAR",
                "EFECTIVO",
                null,
                null,
                null,
                null,
                new BigDecimal("1000"),
                List.of(new OrderDetailRequestDTO(1L, 1)));

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThrows(OrderOperationNotAllowedException.class,
                () -> orderService.updateOrder(10L, request));

        verify(stockService, never()).adjustAvailability(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateStatusRejectsDeliveredToCancelled() {
        Order order = Order.builder()
                .orderId(10L)
                .status(OrderStatus.ENTREGADO)
                .details(new ArrayList<>())
                .build();
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        OrderOperationNotAllowedException error = assertThrows(
                OrderOperationNotAllowedException.class,
                () -> orderService.updateOrderStatus(10L, OrderStatus.CANCELADO));

        assertEquals(
                "No se permite cambiar el pedido de ENTREGADO a CANCELADO",
                error.getMessage());
        verify(stockService, never()).adjustAvailability(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updatePaymentAcceptsCombinedWithValidAmounts() {
        Order order = Order.builder()
                .orderId(10L)
                .status(OrderStatus.PENDIENTE)
                .paymentType(PaymentType.EFECTIVO)
                .cashAmount(new BigDecimal("9000"))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal("9000"))
                .details(new ArrayList<>())
                .build();
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        boolean updated = orderService.updatePaymentType(
                10L,
                PaymentType.COMBINADO,
                new BigDecimal("5000"),
                new BigDecimal("4000"));

        assertEquals(true, updated);
        assertEquals(PaymentType.COMBINADO, order.getPaymentType());
        assertEquals(new BigDecimal("5000"), order.getCashAmount());
        assertEquals(new BigDecimal("4000"), order.getTransferAmount());
        verify(orderRepository).save(order);
    }

    @Test
    void updatingMissingOrderReturnsSpecificException() {
        when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(999L, OrderStatus.PREPARADO));
    }
}
