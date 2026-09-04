package com.bienCriollas.stock.order.service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.order.dto.OrderReconciliationQueryDTO;
import com.bienCriollas.stock.order.dto.OrderReconciliationItemDTO;
import com.bienCriollas.stock.order.dto.OrderReconciliationResultDTO;
import com.bienCriollas.stock.order.dto.ReconcileOrdersRequestDTO;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.entity.OrderReconciliation;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.exception.InvalidOrderException;
import com.bienCriollas.stock.order.exception.OrderNotFoundException;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.order.repository.OrderReconciliationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderReconciliationService {

    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final List<OrderStatus> RECONCILABLE_STATUSES = List.of(
            OrderStatus.PENDIENTE,
            OrderStatus.PREPARADO);
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderReconciliationRepository orderReconciliationRepository;

    @Transactional(readOnly = true)
    public OrderReconciliationQueryDTO queryMonth(
            Integer year,
            Integer month,
            int page,
            int size) {
        YearMonth period = validatePeriod(year, month);
        validatePagination(page, size);

        LocalDate from = period.atDay(1);
        LocalDate to = period.plusMonths(1).atDay(1);
        Pageable pageable = PageRequest.of(
                page,
                size,
				Sort.by(
						Sort.Order.asc("creationDate"),
						Sort.Order.asc("orderId")));

        Page<Order> result = orderRepository
                .findByStatusInAndCreationDateGreaterThanEqualAndCreationDateLessThan(
                        RECONCILABLE_STATUSES,
                        from,
                        to,
                        pageable);
        BigDecimal potentialIncome = orderRepository.sumTotalByStatusesAndPeriod(
                RECONCILABLE_STATUSES,
                from,
                to);

        return new OrderReconciliationQueryDTO(
                period.getYear(),
                period.getMonthValue(),
                result.getTotalElements(),
                potentialIncome == null ? BigDecimal.ZERO : potentialIncome,
                result.getContent().stream().map(this::toItemDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages());
    }

    @Transactional
    public OrderReconciliationResultDTO reconcile(
            ReconcileOrdersRequestDTO request,
            String performedBy) {
        if (request == null) {
            throw new InvalidOrderException("La solicitud de regularización es obligatoria");
        }
        validateRequest(request);
        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new InvalidOrderException("Debés confirmar expresamente la regularización");
        }
        if (performedBy == null || performedBy.isBlank()) {
            throw new InvalidOrderException("No se pudo identificar al administrador");
        }

        YearMonth period = validatePeriod(request.year(), request.month());
        LinkedHashSet<Long> ids = new LinkedHashSet<>(request.orderIds());
        List<Order> orders = orderRepository.findAllByIdForReconciliation(ids);

        validateFoundOrders(ids, orders);
        validateReconcilableOrders(period, orders);

        String batchId = UUID.randomUUID().toString();
        OffsetDateTime performedAt = OffsetDateTime.now(ARGENTINA_ZONE);
        String reason = request.reason().trim();
        BigDecimal incorporatedIncome = BigDecimal.ZERO;
        List<OrderReconciliation> auditEntries = new ArrayList<>(orders.size());

        for (Order order : orders) {
            OrderStatus previousStatus = order.getStatus();
            incorporatedIncome = incorporatedIncome.add(order.getOrderTotal());
            order.setStatus(OrderStatus.ENTREGADO);
            auditEntries.add(OrderReconciliation.builder()
                    .batchId(batchId)
                    .order(order)
                    .previousStatus(previousStatus)
                    .newStatus(OrderStatus.ENTREGADO)
                    .performedBy(performedBy)
                    .reason(reason)
                    .performedAt(performedAt)
                    .build());
        }

        orderRepository.saveAll(orders);
        orderReconciliationRepository.saveAll(auditEntries);

        List<Long> updatedIds = orders.stream()
                .map(Order::getOrderId)
                .sorted()
                .toList();
        return new OrderReconciliationResultDTO(
                batchId,
                period.getYear(),
                period.getMonthValue(),
                orders.size(),
                incorporatedIncome,
                updatedIds,
                performedBy,
                performedAt);
    }

    private OrderReconciliationItemDTO toItemDto(Order order) {
        return new OrderReconciliationItemDTO(
                order.getOrderId(),
                order.getCreationDate(),
                order.getCustomer(),
                order.getSaleType(),
                order.getPaymentType(),
				order.getPedidosYaOrderNumber(),
                order.getDeliveryTime(),
                order.getOrderTotal(),
                order.getStatus());
    }

    private YearMonth validatePeriod(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new InvalidOrderException("El año y el mes son obligatorios");
        }
        if (year < 2000 || year > 2100) {
            throw new InvalidOrderException("El año debe estar entre 2000 y 2100");
        }
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new InvalidOrderException("El año o el mes no son válidos", exception);
        }
    }

    private void validateRequest(ReconcileOrdersRequestDTO request) {
        if (request.orderIds() == null || request.orderIds().isEmpty()) {
            throw new InvalidOrderException("Debés seleccionar al menos un pedido");
        }
        if (request.orderIds().size() > 200) {
            throw new InvalidOrderException("No se pueden reconcile más de 200 pedidos por operación");
        }
        for (Long id : request.orderIds()) {
            if (id == null || id.longValue() <= 0) {
                throw new InvalidOrderException("Todos los IDs de pedido deben ser válidos");
            }
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidOrderException("El motivo de la regularización es obligatorio");
        }
        if (request.reason().trim().length() > 250) {
            throw new InvalidOrderException("El motivo no puede superar los 250 caracteres");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidOrderException("La página no puede ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidOrderException("El tamaño de página debe estar entre 1 y 100");
        }
    }

    private void validateFoundOrders(Set<Long> requestedIds, List<Order> orders) {
        Set<Long> foundIds = new HashSet<>();
        for (Order order : orders) {
            foundIds.add(order.getOrderId());
        }
        for (Long id : requestedIds) {
            if (!foundIds.contains(id)) {
                throw new OrderNotFoundException(id);
            }
        }
    }

    private void validateReconcilableOrders(YearMonth period, List<Order> orders) {
        for (Order order : orders) {
            if (order.getCreationDate() == null
                    || !YearMonth.from(order.getCreationDate()).equals(period)) {
                throw new OrderOperationNotAllowedException(
                        "El pedido " + order.getOrderId() + " no pertenece al período seleccionado");
            }
            if (!RECONCILABLE_STATUSES.contains(order.getStatus())) {
                throw new OrderOperationNotAllowedException(
                        "El pedido " + order.getOrderId()
                                + " ya no está PENDIENTE o PREPARADO");
            }
            if (order.getOrderTotal() == null || order.getOrderTotal().signum() < 0) {
                throw new OrderOperationNotAllowedException(
                        "El pedido " + order.getOrderId() + " tiene un total inválido");
            }
        }
    }
}
