package com.bienCriollas.stock.order.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.income.dto.DailyIncomeDTO;
import com.bienCriollas.stock.order.dto.OrderDetailRequestDTO;
import com.bienCriollas.stock.order.dto.OrderDetailResponseDTO;
import com.bienCriollas.stock.order.dto.OrderRequestDTO;
import com.bienCriollas.stock.order.dto.OrderResponseDTO;
import com.bienCriollas.stock.order.interfaces.IOrderService;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.exception.InvalidOrderException;
import com.bienCriollas.stock.order.exception.OrderNotFoundException;

import com.bienCriollas.stock.order.entity.OrderDetail;
import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;

import com.bienCriollas.stock.order.repository.OrderDetailRepository;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;
import com.bienCriollas.stock.stock.service.StockService;

import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;


    private final StockService stockService;

    private final OrderDetailRepository orderDetailRepository;

    private final EmpanadaVarietyRepository empanadaVarietyRepository;



    //Metodo para create un nuevo pedido

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequest) {



        if (orderRequest == null) {
            throw new InvalidOrderException("El pedido no puede ser nulo");
        }
        validateOrderForUpdate(orderRequest);

        // ✅ Fecha de hoy en Argentina (evita desfasajes en Railway)
        LocalDate currentDate = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));


     // 1) Parsear enums una sola vez
        SaleType saleType = parseEnum(SaleType.class, orderRequest.saleType(), "tipo de venta");
        PaymentType paymentType = parseEnum(PaymentType.class, orderRequest.paymentType(), "tipo de pago");


        //Calcular montos según tipo de pago
        BigDecimal cashAmount = BigDecimal.ZERO;
        BigDecimal transferAmount = BigDecimal.ZERO;

        if (paymentType != null) {
            switch (paymentType) {
                case EFECTIVO:
                    cashAmount = orderRequest.orderTotal();
                    transferAmount = BigDecimal.ZERO;
                    break;

                case TRANSFERENCIA:
                    cashAmount = BigDecimal.ZERO;
                    transferAmount = orderRequest.orderTotal();
                    break;

                case COMBINADO:
                    BigDecimal requestCashAmount = orderRequest.cashAmount() != null
                            ? orderRequest.cashAmount()
                            : BigDecimal.ZERO;

                    BigDecimal requestTransferAmount = orderRequest.transferAmount() != null
                            ? orderRequest.transferAmount()
                            : BigDecimal.ZERO;

                    if (requestCashAmount.compareTo(BigDecimal.ZERO) < 0 || requestTransferAmount.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InvalidOrderException("Los montos de pago no pueden ser negativos");
                    }

                    BigDecimal sum = requestCashAmount.add(requestTransferAmount);
                    if (sum.compareTo(orderRequest.orderTotal()) != 0) {
                        throw new InvalidOrderException(
                                "La suma de efectivo + transferencia (" + sum +
                                ") debe ser igual al total del pedido (" + orderRequest.orderTotal() + ")");
                    }

                    cashAmount = requestCashAmount;
                    transferAmount = requestTransferAmount;
                    break;

                default:
                    break;
            }
        }

        // Creamos el pedidos con estado PENDIENTE (siempre se guarda en pendiente)

        Order newOrder = Order.builder()
                .customer(orderRequest.customer())
                .saleType(saleType)
                .paymentType(paymentType)
                .pedidosYaOrderNumber(orderRequest.pedidosYaOrderNumber() != null ? orderRequest.pedidosYaOrderNumber() : null)
                .deliveryTime(orderRequest.deliveryTime())
                .creationDate(currentDate)
                .cashAmount(cashAmount)
                .transferAmount(transferAmount)
                .orderTotal(orderRequest.orderTotal() != null ? orderRequest.orderTotal() : BigDecimal.ZERO)
                .status(OrderStatus.PENDIENTE)
                .build();

        // Guardamos el pedido para obtener su ID y poder asociar los detalles


        Order savedOrder = orderRepository.save(newOrder);

        // Agrupamos y bloqueamos todas las variedades en orden antes de descontar.
        // Así dos pedidos simultáneos nunca pisan el valor de stock del otro.
        List<OrderDetailRequestDTO> orderDetails = orderRequest.details();
        Map<Long, EmpanadaVariety> varieties = new HashMap<>();
        TreeMap<Long, Integer> quantities = new TreeMap<>();

        for (OrderDetailRequestDTO det : orderDetails) {
            EmpanadaVariety variety = empanadaVarietyRepository.findById(det.varietyId())
                .orElseThrow(() -> new VarietyNotFoundException(det.varietyId()));
            varieties.put(det.varietyId(), variety);
            accumulateQuantity(quantities, det.varietyId(), det.quantity());
        }

        TreeMap<Long, Integer> deductions = new TreeMap<>();
        quantities.forEach((varietyId, quantity) -> deductions.put(varietyId, -quantity));
        stockService.adjustAvailability(deductions);

        for (OrderDetailRequestDTO det : orderDetails) {
            OrderDetail detail = OrderDetail.builder()
                    .order(newOrder)
                    .variety(varieties.get(det.varietyId()))
                    .quantity(det.quantity())
                    .build();
            orderDetailRepository.save(detail);
            newOrder.getDetails().add(detail);
        }

        //Retornamos el pedido guardado como DTO
        return new OrderResponseDTO(
                savedOrder.getOrderId(),
                savedOrder.getCustomer(),
                savedOrder.getSaleType() != null ? savedOrder.getSaleType().name() : null,
                savedOrder.getPaymentType() != null ? savedOrder.getPaymentType().name() : null,
                savedOrder.getPedidosYaOrderNumber(),
                savedOrder.getDeliveryTime(),
                savedOrder.getOrderTotal(),
                savedOrder.getStatus()
        );


    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrder(Long orderId, OrderRequestDTO orderRequest) {
        if (orderRequest == null) {
            throw new InvalidOrderException("El pedido no puede ser nulo");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDIENTE && order.getStatus() != OrderStatus.PREPARADO) {
            throw new OrderOperationNotAllowedException(
                    "Solo se puede editar un pedido que esté PENDIENTE o PREPARADO");
        }

        validateOrderForUpdate(orderRequest);

        SaleType saleType = parseEnum(SaleType.class, orderRequest.saleType(), "tipo de venta");
        PaymentType paymentType = parseEnum(PaymentType.class, orderRequest.paymentType(), "tipo de pago");
        PaymentAmounts amounts = calculatePaymentAmounts(orderRequest, paymentType);

        Map<Long, EmpanadaVariety> newVarieties = new HashMap<>();
        TreeMap<Long, Integer> stockChanges = getOrderQuantities(order);

        for (OrderDetailRequestDTO detailRequest : orderRequest.details()) {
            EmpanadaVariety variety = empanadaVarietyRepository.findById(detailRequest.varietyId())
                    .orElseThrow(() -> new VarietyNotFoundException(detailRequest.varietyId()));
            newVarieties.put(detailRequest.varietyId(), variety);
            accumulateQuantity(stockChanges, detailRequest.varietyId(), -detailRequest.quantity());
        }

        // Se aplica solamente la diferencia neta entre el pedido anterior y el nuevo.
        // El servicio de stock bloquea todas las variedades en un orden estable.
        stockService.adjustAvailability(stockChanges);
        order.getDetails().clear();

        order.setCustomer(orderRequest.customer().trim());
        order.setSaleType(saleType);
        order.setPaymentType(paymentType);
        order.setPedidosYaOrderNumber(orderRequest.pedidosYaOrderNumber());
        order.setDeliveryTime(orderRequest.deliveryTime());
        order.setCashAmount(amounts.cash());
        order.setTransferAmount(amounts.transfer());
        order.setOrderTotal(orderRequest.orderTotal());

        for (OrderDetailRequestDTO detailRequest : orderRequest.details()) {
            order.getDetails().add(OrderDetail.builder()
                    .order(order)
                    .variety(newVarieties.get(detailRequest.varietyId()))
                    .quantity(detailRequest.quantity())
                    .build());
        }

        Order updatedOrder = orderRepository.save(order);
        return toDto(updatedOrder);
    }




    //Metodo para actualizar el estado de un pedido

    @Override
    @Transactional
    public boolean updateOrderStatus(Long orderId, OrderStatus newStatus) {

        if (newStatus == null) {
            throw new InvalidOrderException("El nuevo estado es obligatorio");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == newStatus) {
            return true;
        }

        if (!isTransitionAllowed(previousStatus, newStatus)) {
            throw new OrderOperationNotAllowedException(
                    "No se permite cambiar el pedido de " + previousStatus + " a " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELADO) {

            returnStockForCancellation(order);

            // ✅ Si es PEDIDOS_YA, liberamos el número para poder reutilizarlo
            if (order.getSaleType() == SaleType.PEDIDOS_YA) {
                order.setPedidosYaOrderNumber(null);
            }

            // ✅ Al cancelar, resetear montos
            order.setCashAmount(BigDecimal.ZERO);
            order.setTransferAmount(BigDecimal.ZERO);
            order.setOrderTotal(BigDecimal.ZERO);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        return true;
    }


    @Override
    @Transactional
    public boolean updatePaymentType(
            Long orderId,
            PaymentType newPaymentType,
            BigDecimal cashAmount,
            BigDecimal transferAmount) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDIENTE && order.getStatus() != OrderStatus.PREPARADO) {
            throw new OrderOperationNotAllowedException(
                    "Solo se puede cambiar el tipo de pago si el pedido está PENDIENTE o PREPARADO");
        }

        if (newPaymentType == null) {
            throw new InvalidOrderException("El nuevo tipo de pago no puede ser nulo");
        }

        BigDecimal total = order.getOrderTotal();

        switch (newPaymentType) {
            case EFECTIVO -> {
                order.setPaymentType(PaymentType.EFECTIVO);
                order.setCashAmount(total);
                order.setTransferAmount(BigDecimal.ZERO);
            }
            case TRANSFERENCIA -> {
                order.setPaymentType(PaymentType.TRANSFERENCIA);
                order.setCashAmount(BigDecimal.ZERO);
                order.setTransferAmount(total);
            }
            case COMBINADO -> {
                if (cashAmount == null || transferAmount == null) {
                    throw new InvalidOrderException(
                            "Para COMBINADO tenés que enviar montoEfectivo y montoTransferencia");
                }
                if (cashAmount.compareTo(BigDecimal.ZERO) < 0
                        || transferAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidOrderException("Los montos de pago no pueden ser negativos");
                }
                if (cashAmount.add(transferAmount).compareTo(total) != 0) {
                    throw new InvalidOrderException(
                            "La suma de efectivo + transferencia debe ser igual al total del pedido");
                }

                order.setPaymentType(PaymentType.COMBINADO);
                order.setCashAmount(cashAmount);
                order.setTransferAmount(transferAmount);
            }
        }

        orderRepository.save(order);
        return true;
    }

    //Metodo para obtener todos los pedidos, con filtro opcional por estado
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders(OrderStatus status) {

        List<Order> orders;

        if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream()
                .map(order -> new OrderResponseDTO(
                        order.getOrderId(),
                        order.getCustomer(),
                        order.getSaleType() != null ? order.getSaleType().name() : null,
                        order.getPaymentType() != null ? order.getPaymentType().name() : null,
                        order.getPedidosYaOrderNumber(),
                        order.getDeliveryTime(),
                        order.getOrderTotal(),
                        order.getStatus()
                ))
                .toList();

}

    //Metodo para obtener todos los pedidos por fecha de creacion

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByDate(LocalDate startDate) {

        List<Order> orders = orderRepository.findByCreationDate(startDate);

        return orders.stream()
                .map(order -> new OrderResponseDTO(
                        order.getOrderId(),
                        order.getCustomer(),
                        order.getSaleType() != null ? order.getSaleType().name() : null,
                        order.getPaymentType() != null ? order.getPaymentType().name() : null,
                        order.getPedidosYaOrderNumber(),
                        order.getDeliveryTime(),
                        order.getOrderTotal(),
                        order.getStatus()
                ))
                .toList();
}

    //Metodo para obtener los detalles de un pedido por su id

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailResponseDTO> getOrderDetails(Long orderId) {

        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<OrderDetail> details = existingOrder.getDetails();

        if (details.isEmpty()) {
            throw new OrderNotFoundException(
                    "No se encontraron detalles para el pedido con id " + orderId);
        }

        return details.stream()
                .map(detail -> new OrderDetailResponseDTO(
                        existingOrder.getCustomer(),
                        detail.getVariety().getVarietyId(),
                        detail.getVariety().getName(),
                        detail.getQuantity(),
                        existingOrder.getOrderTotal(),
                        existingOrder.getSaleType(),
                        existingOrder.getPaymentType()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getPagedOrders(OrderStatus status, int page, int size) {

        // ORDEN personalizado
        Sort sort = Sort.by(
                Sort.Order.by("saleType").with(Sort.Direction.ASC),
                Sort.Order.by("deliveryTime").with(Sort.Direction.ASC),
                Sort.Order.by("orderId").with(Sort.Direction.DESC)
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        // 🔹 solo pedidos del día (fechaCreacion = hoy)
        LocalDate today = LocalDate.now();
        // si querés zona explícita:
        // LocalDate hoy = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));

        Page<Order> orders =
                orderRepository.findByStatusAndCreationDate(status, today, pageable);

        return orders.map(p -> new OrderResponseDTO(
                p.getOrderId(),
                p.getCustomer(),
                p.getSaleType() != null ? p.getSaleType().name() : null,
                p.getPaymentType() != null ? p.getPaymentType().name() : null,
                p.getPedidosYaOrderNumber(),
                p.getDeliveryTime(),
                p.getOrderTotal(),
                p.getStatus()
        ));
    }





    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getPagedOrdersByStatusAndDate(
            OrderStatus status,
            LocalDate date,   // la fecha seleccionada en caja
            int page,
            int size
    ) {
        if (date == null) {
            date = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                    Sort.by(Sort.Direction.DESC, "orderId")
        );

        return orderRepository.findByStatusAndCreationDate(status, date, pageable)
                .map(this::toDto);
    }

    private OrderResponseDTO toDto(Order p) {
        return new OrderResponseDTO(
                p.getOrderId(),
                p.getCustomer(), // o p.getCustomer().getNombre() si tenés entidad Cliente
                p.getSaleType() == null ? null : p.getSaleType().name(), // si es enum
                p.getPaymentType()  == null ? null : p.getPaymentType().name(),  // si es enum
                    p.getPedidosYaOrderNumber(), // tu campo de PedidosYa
                p.getDeliveryTime(),
                p.getOrderTotal(),
                p.getStatus()
        );
    }


    @Override
    public DailyIncomeDTO calculateDailyIncome(LocalDate date, OrderStatus status) {

        List<Order> orders = orderRepository.findByCreationDateAndStatus(date, status);
        if(orders.isEmpty()) {
            throw new OrderNotFoundException(
                    "No se encontraron pedidos para la fecha " + date + " y estado " + status);
        }

        BigDecimal cashIncome = BigDecimal.ZERO;
        BigDecimal transferIncome = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order.getSaleType() == SaleType.PEDIDOS_YA) {
                continue;
            }
            if (order.getPaymentType() == PaymentType.EFECTIVO
                    || order.getPaymentType() == PaymentType.COMBINADO) {
                cashIncome = cashIncome.add(order.getCashAmount());
            }
            if (order.getPaymentType() == PaymentType.TRANSFERENCIA
                    || order.getPaymentType() == PaymentType.COMBINADO) {
                transferIncome = transferIncome.add(
                        order.getTransferAmount());
            }
        }

        BigDecimal totalIncome = cashIncome.add(transferIncome);

        return new DailyIncomeDTO(cashIncome, transferIncome, totalIncome);
    }


    private void returnStockForCancellation(Order order) {
        stockService.adjustAvailability(getOrderQuantities(order));
    }

    private TreeMap<Long, Integer> getOrderQuantities(Order order) {
        TreeMap<Long, Integer> quantities = new TreeMap<>();
        for (OrderDetail detail : order.getDetails()) {
            accumulateQuantity(
                    quantities,
                    detail.getVariety().getVarietyId(),
                    detail.getQuantity());
        }
        return quantities;
    }

    private void accumulateQuantity(Map<Long, Integer> quantities, Long varietyId, int change) {
        Integer currentQuantity = quantities.get(varietyId);
        quantities.put(
                varietyId,
                currentQuantity == null ? change : Math.addExact(currentQuantity, change));
    }

    private boolean isTransitionAllowed(OrderStatus currentStatus, OrderStatus newStatus) {
        return switch (currentStatus) {
            case PENDIENTE -> newStatus == OrderStatus.PREPARADO
                    || newStatus == OrderStatus.CANCELADO;
            case PREPARADO -> newStatus == OrderStatus.ENTREGADO
                    || newStatus == OrderStatus.CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }

    private void validateOrderForUpdate(OrderRequestDTO orderRequest) {
        if (orderRequest.customer() == null || orderRequest.customer().isBlank()) {
            throw new InvalidOrderException("El nombre del cliente es obligatorio");
        }
        if (orderRequest.orderTotal() == null || orderRequest.orderTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderException("El total del pedido no puede ser nulo ni negativo");
        }
        if (orderRequest.details() == null || orderRequest.details().isEmpty()) {
            throw new InvalidOrderException("El pedido debe tener al menos un detalle");
        }

        for (OrderDetailRequestDTO detail : orderRequest.details()) {
            if (detail == null || detail.varietyId() == null) {
                throw new InvalidOrderException("Cada detalle debe indicar una variedad");
            }
            if (detail.quantity() == null || detail.quantity() <= 0) {
                throw new InvalidOrderException("La cantidad de cada detalle debe ser mayor a cero");
            }
        }
    }

    private PaymentAmounts calculatePaymentAmounts(OrderRequestDTO orderRequest, PaymentType paymentType) {
        return switch (paymentType) {
            case EFECTIVO -> new PaymentAmounts(orderRequest.orderTotal(), BigDecimal.ZERO);
            case TRANSFERENCIA -> new PaymentAmounts(BigDecimal.ZERO, orderRequest.orderTotal());
            case COMBINADO -> {
                BigDecimal cash = orderRequest.cashAmount() == null
                        ? BigDecimal.ZERO
                        : orderRequest.cashAmount();
                BigDecimal transfer = orderRequest.transferAmount() == null
                        ? BigDecimal.ZERO
                        : orderRequest.transferAmount();

                if (cash.compareTo(BigDecimal.ZERO) < 0
                        || transfer.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidOrderException("Los montos de pago no pueden ser negativos");
                }

                BigDecimal sum = cash.add(transfer);
                if (sum.compareTo(orderRequest.orderTotal()) != 0) {
                    throw new InvalidOrderException(
                            "La suma de efectivo + transferencia debe ser igual al total del pedido");
                }

                yield new PaymentAmounts(cash, transfer);
            }
        };
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException("El " + fieldName + " es obligatorio");
        }

        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("El " + fieldName + " no es válido: " + value);
        }
    }

    private record PaymentAmounts(BigDecimal cash, BigDecimal transfer) {
    }



}
