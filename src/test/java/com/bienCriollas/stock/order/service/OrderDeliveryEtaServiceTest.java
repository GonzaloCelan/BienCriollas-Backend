package com.bienCriollas.stock.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bienCriollas.stock.order.dto.OrderDetailRequestDTO;
import com.bienCriollas.stock.order.dto.OrderRequestDTO;
import com.bienCriollas.stock.order.dto.OrderResponseDTO;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.bienCriollas.stock.order.exception.InvalidOrderException;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.repository.OrderDetailRepository;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.stock.service.StockService;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryEtaServiceTest {

    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock private OrderRepository orderRepository;
    @Mock private StockService stockService;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private EmpanadaVarietyRepository varietyRepository;
    @Mock private StockRepository stockRepository;
    @InjectMocks private OrderService orderService;

    @Test
    void createsPedidosYaWithOptionalEtaAndReturnsTargetDateTime() {
        prepareCreation();
        LocalDateTime before = argentinaNow().plusMinutes(31);

        OrderResponseDTO response = orderService.createOrder(request("PEDIDOS_YA", 31));

        LocalDateTime after = argentinaNow().plusMinutes(31);
        assertThat(response.estimatedDeliveryDateTime()).isBetween(before, after);
        assertThat(response.estimatedDeliveryDateTime().getNano()).isZero();
    }

    @Test
    void createsPedidosYaWithoutEtaAndParticularAlwaysWithoutEta() {
        prepareCreation();

        assertThat(orderService.createOrder(request("PEDIDOS_YA", null))
                .estimatedDeliveryDateTime()).isNull();
        assertThat(orderService.createOrder(request("PARTICULAR", null))
                .estimatedDeliveryDateTime()).isNull();
        assertThat(orderService.createOrder(request("PARTICULAR", 20))
                .estimatedDeliveryDateTime()).isNull();
    }

    @Test
    void rejectsZeroAndNegativeMinutes() {
        assertThatThrownBy(() -> orderService.createOrder(request("PEDIDOS_YA", 0)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("El tiempo estimado del delivery debe ser mayor a cero.");
        assertThatThrownBy(() -> orderService.createOrder(request("PEDIDOS_YA", -5)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("El tiempo estimado del delivery debe ser mayor a cero.");
    }

    @Test
    void patchRecalculatesAndRemovesEtaWithoutChangingBusinessState() {
        LocalDateTime oldEta = argentinaNow().plusMinutes(10);
        Order order = order(1L, SaleType.PEDIDOS_YA, OrderStatus.PREPARADO, false, oldEta);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        LocalDateTime before = argentinaNow().plusMinutes(20);

        OrderResponseDTO updated = orderService.updateDeliveryEta(1L, 20);

        LocalDateTime after = argentinaNow().plusMinutes(20);
        assertThat(updated.estimatedDeliveryDateTime()).isBetween(before, after);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARADO);
        assertThat(order.isPagado()).isFalse();
        verify(stockService, never()).adjustAvailability(any());

        OrderResponseDTO removed = orderService.updateDeliveryEta(1L, null);
        assertThat(removed.estimatedDeliveryDateTime()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARADO);
        assertThat(order.isPagado()).isFalse();
    }

    @Test
    void patchRejectsInvalidMinutesAndParticularOrders() {
        Order pedidosYa = order(1L, SaleType.PEDIDOS_YA, OrderStatus.PENDIENTE, true, null);
        Order particular = order(2L, SaleType.PARTICULAR, OrderStatus.PENDIENTE, true, null);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedidosYa));
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(particular));

        assertThatThrownBy(() -> orderService.updateDeliveryEta(1L, 0))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("El tiempo estimado del delivery debe ser mayor a cero.");
        assertThatThrownBy(() -> orderService.updateDeliveryEta(2L, 20))
                .isInstanceOf(OrderOperationNotAllowedException.class)
                .hasMessage("El tiempo estimado del delivery solo aplica a pedidos de PedidosYa.");
        verify(orderRepository, never()).save(particular);
    }

    @Test
    void stateChangesPreserveEtaWhenPreparedDeliveredOrCancelled() {
        LocalDateTime eta = argentinaNow().plusMinutes(31);
        Order delivered = order(1L, SaleType.PEDIDOS_YA, OrderStatus.PENDIENTE, false, eta);
        Order cancelled = order(2L, SaleType.PEDIDOS_YA, OrderStatus.PENDIENTE, false, eta);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivered));
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(cancelled));

        orderService.updateOrderStatus(1L, OrderStatus.PREPARADO);
        assertThat(delivered.getFechaHoraEstimadaDelivery()).isEqualTo(eta);
        orderService.updateOrderStatus(1L, OrderStatus.ENTREGADO);
        assertThat(delivered.getFechaHoraEstimadaDelivery()).isEqualTo(eta);

        orderService.updateOrderStatus(2L, OrderStatus.CANCELADO);
        assertThat(cancelled.getFechaHoraEstimadaDelivery()).isEqualTo(eta);
    }

    private void prepareCreation() {
        EmpanadaVariety variety = EmpanadaVariety.builder().varietyId(1L).name("Carne").build();
        when(varietyRepository.findById(1L)).thenReturn(Optional.of(variety));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(10L);
            return order;
        });
    }

    private OrderRequestDTO request(String saleType, Integer minutes) {
        return new OrderRequestDTO(
                "Cliente", saleType, "EFECTIVO", "PYA-1", null,
                null, null, new BigDecimal("18000"),
                List.of(new OrderDetailRequestDTO(1L, 12)), null, false, minutes);
    }

    private Order order(
            Long id,
            SaleType saleType,
            OrderStatus status,
            boolean pagado,
            LocalDateTime eta) {
        return Order.builder()
                .orderId(id)
                .customer("Cliente")
                .saleType(saleType)
                .paymentType(PaymentType.EFECTIVO)
                .cashAmount(new BigDecimal("18000"))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal("18000"))
                .status(status)
                .pagado(pagado)
                .fechaHoraEstimadaDelivery(eta)
                .stockDiscounted(true)
                .details(new ArrayList<>())
                .build();
    }

    private LocalDateTime argentinaNow() {
        return LocalDateTime.now(ARGENTINA_ZONE).truncatedTo(ChronoUnit.SECONDS);
    }
}
