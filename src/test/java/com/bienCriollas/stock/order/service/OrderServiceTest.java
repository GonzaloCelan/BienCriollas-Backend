package com.bienCriollas.stock.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
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
import com.bienCriollas.stock.order.entity.OrderDetail;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.exception.OrderNotFoundException;
import com.bienCriollas.stock.order.repository.OrderDetailRepository;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.stock.service.StockService;
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
                "Cliente",
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
        verify(orderRepository).save(any(Order.class));
        verify(stockService).adjustAvailability(eq(Map.of(2L, -6)));
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
                "Cliente actualizado",
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

        assertEquals("Cliente actualizado", order.getCustomer());
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
