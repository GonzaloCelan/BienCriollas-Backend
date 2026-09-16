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

    @Query("""
            select o
            from Order o
            where o.status = :status
              and (
                    o.deliveryDate = :orderDate
                    or (
                        o.creationDate = :orderDate
                        and (o.deliveryDate is null or o.deliveryDate <= :orderDate)
                    )
                  )
            """)
    Page<Order> findDailyOrdersByStatus(
            @Param("status") OrderStatus status,
            @Param("orderDate") LocalDate orderDate,
            Pageable pageable);

    @Query("""
            select o
            from Order o
            where o.deliveryDate = :date
               or (
                    o.creationDate = :date
                    and (o.deliveryDate is null or o.deliveryDate <= :date)
                  )
            order by o.deliveryTime asc, o.orderId desc
            """)
    List<Order> findDailyOrders(@Param("date") LocalDate date);

    @Query("""
            select o
            from Order o
            where o.deliveryDate > :today
              and o.status <> :cancelledStatus
            order by o.deliveryDate asc, o.deliveryTime asc, o.orderId asc
            """)
    List<Order> findScheduledOrdersAfter(
            @Param("today") LocalDate today,
            @Param("cancelledStatus") OrderStatus cancelledStatus);

    @Query("""
            select o
            from Order o
            where o.deliveryDate = :deliveryDate
              and o.status <> :cancelledStatus
            order by o.deliveryTime asc, o.orderId asc
            """)
    List<Order> findScheduledOrdersOn(
            @Param("deliveryDate") LocalDate deliveryDate,
            @Param("cancelledStatus") OrderStatus cancelledStatus);

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
