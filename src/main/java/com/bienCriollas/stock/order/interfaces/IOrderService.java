package com.bienCriollas.stock.order.interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.bienCriollas.stock.income.dto.DailyIncomeDTO;
import com.bienCriollas.stock.order.dto.OrderDetailResponseDTO;
import com.bienCriollas.stock.order.dto.OrderRequestDTO;
import com.bienCriollas.stock.order.dto.OrderResponseDTO;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;

public interface IOrderService {

      public OrderResponseDTO createOrder(OrderRequestDTO orderRequest);

      public OrderResponseDTO updateOrder(Long orderId, OrderRequestDTO orderRequest);

      public boolean updateOrderStatus(Long orderId, OrderStatus newStatus);

      public boolean updatePaymentType(
              Long orderId,
              PaymentType newPaymentType,
              BigDecimal cashAmount,
              BigDecimal transferAmount);

      public DailyIncomeDTO calculateDailyIncome(LocalDate date , OrderStatus status);

      public List<OrderResponseDTO> getAllOrders(OrderStatus status);

      public List<OrderResponseDTO> getOrdersByDate(LocalDate startDate);

      public List<OrderDetailResponseDTO> getOrderDetails(Long orderId);

      public Page<OrderResponseDTO> getPagedOrders(OrderStatus status, int page, int size);

      public Page<OrderResponseDTO> getPagedOrdersByStatusAndDate(OrderStatus status, LocalDate date, int page,
            int size);
    }
