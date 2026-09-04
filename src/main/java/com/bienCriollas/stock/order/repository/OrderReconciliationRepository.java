package com.bienCriollas.stock.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.order.entity.OrderReconciliation;

public interface OrderReconciliationRepository extends JpaRepository<OrderReconciliation, Long> {
}
