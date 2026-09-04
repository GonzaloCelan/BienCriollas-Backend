package com.bienCriollas.stock.order.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.SaleType;

import jakarta.persistence.LockModeType;


public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId in :orderIds")
    List<Order> findAllByIdForReconciliation(@Param("orderIds") Collection<Long> orderIds);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCreationDate(LocalDate creationDate);

    List<Order> findByCreationDateAndStatus(LocalDate creationDate, OrderStatus status);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);


     @Query(
                value = "SELECT SUM(p.total) " +
                        "FROM pedido p " +
                        "WHERE DATE(p.fecha_pedido) = :fecha " +
                        "AND p.tipo_pago = :medio " +
                        "AND p.estado = 'ENTREGADO'",
                nativeQuery = true
        )
        Optional<BigDecimal> totalByDateAndPaymentMethod(
                @Param("fecha") LocalDate date,
                @Param("medio") String medio
        );


     @Query(value = """
                SELECT COALESCE(SUM(total_pedido), 0)
                FROM pedido
                WHERE estado = 'ENTREGADO'
                  AND tipo_venta = 'PEDIDOS_YA'
                  AND DATE(fecha_pedido) = :fecha
                """, nativeQuery = true)
            BigDecimal totalDeliveredPedidosYaByDate(@Param("fecha") LocalDate date);


     @Query(value = """
              SELECT COALESCE(SUM(total_pedido), 0)
              FROM pedido
              WHERE estado = 'ENTREGADO'
                AND tipo_venta = 'PEDIDOS_YA'
                AND fecha_pedido >= :desde
                AND fecha_pedido <  :hasta
              """, nativeQuery = true)
          BigDecimal totalDeliveredPedidosYaBetween(
              @Param("desde") LocalDate from,
              @Param("hasta") LocalDate to
          );

     Page<Order> findByStatusAndSaleTypeAndCreationDateGreaterThanEqualAndCreationDateLessThanOrderByCreationDateDesc(
                OrderStatus status,
                SaleType saleType,
                LocalDate from,
                LocalDate to,
                Pageable pageable
            );

     @Query(value = """
              SELECT COALESCE(COUNT(*), 0)
              FROM pedido
              WHERE estado = 'ENTREGADO'
                AND tipo_venta = 'PEDIDOS_YA'
                AND fecha_pedido >= :desde
                AND fecha_pedido <  :hasta
              """, nativeQuery = true)
             Integer countDeliveredPedidosYaBetween(
                @Param("desde") LocalDate from,
                @Param("hasta") LocalDate to
            );

     @Query(value = """
              SELECT COALESCE(COUNT(*), 0)
              FROM pedido
              WHERE estado = 'ENTREGADO'
                AND tipo_venta = 'PARTICULAR'
                AND fecha_pedido >= :desde
                AND fecha_pedido <  :hasta
              """, nativeQuery = true)
            Integer countDeliveredDirectOrdersBetween(
                @Param("desde") LocalDate from,
                @Param("hasta") LocalDate to
            );

     Page<Order> findByStatusAndCreationDate(OrderStatus status, LocalDate orderDate, Pageable pageable);

    Page<Order> findByStatusInAndCreationDateGreaterThanEqualAndCreationDateLessThan(
            Collection<OrderStatus> statuses,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    @Query("""
            select coalesce(sum(o.orderTotal), 0)
            from Order o
            where o.status in :statuses
              and o.creationDate >= :from
              and o.creationDate < :to
            """)
    BigDecimal sumTotalByStatusesAndPeriod(
            @Param("statuses") Collection<OrderStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);


}
