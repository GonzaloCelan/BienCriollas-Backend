package com.bienCriollas.stock.order.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.order.entity.OrderDetail;


public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {



    List<OrderDetail> findByOrderOrderId(Long orderId);

    @Query("""
            select detail.variety.varietyId,
                   detail.variety.name,
                   sum(detail.quantity)
            from OrderDetail detail
            where detail.order.deliveryDate > :today
              and detail.order.status not in :excludedStatuses
              and detail.order.stockDiscounted = false
            group by detail.variety.varietyId, detail.variety.name
            order by detail.variety.name asc
            """)
    List<Object[]> sumCommittedStockByVariety(
            @Param("today") LocalDate today,
            @Param("excludedStatuses") Collection<com.bienCriollas.stock.order.enums.OrderStatus> excludedStatuses);


    @Query("SELECT COALESCE(SUM(detail.quantity), 0) " +
               "FROM OrderDetail detail " +
               "WHERE detail.order.creationDate = :date")
    Integer getTotalSoldEmpanadasByDate(@Param("date") LocalDate date);



    @Query(value = """
            SELECT
                v.nombre AS variedad,
                SUM(d.cantidad) AS total_vendidas
            FROM pedido_detalle d
            JOIN pedido p ON p.id_pedido = d.id_pedido
            JOIN variedad_empanada v ON v.id_variedad = d.id_variedad
            WHERE DATE(p.fecha_pedido) = :fecha
              AND p.estado = :estado
            GROUP BY v.nombre
            ORDER BY total_vendidas DESC
        """, nativeQuery = true)
        List<Object[]> getTotalEmpanadasByVarietyDateAndStatus(
                @Param("fecha") LocalDate date,
                @Param("estado") String status
        );

}
