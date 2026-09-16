package com.bienCriollas.stock.production.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.production.entity.Production;
import com.bienCriollas.stock.production.enums.ProductionStatus;

import jakarta.persistence.LockModeType;

public interface ProductionRepository extends JpaRepository<Production, Long> {

    @EntityGraph(attributePaths = {"variety", "recipe", "process", "ingredients", "ingredients.ingredient"})
    @Query("SELECT DISTINCT p FROM Production p WHERE p.id = :id")
    Optional<Production> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Production p WHERE p.id = :id")
    Optional<Production> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"variety", "recipe", "process"})
    Page<Production> findByStatus(ProductionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"variety", "recipe", "process"})
    Page<Production> findByVarietyVarietyId(Long varietyId, Pageable pageable);

    @EntityGraph(attributePaths = {"variety", "recipe", "process"})
    Page<Production> findByProductionDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = {"variety", "recipe", "process"})
    Page<Production> findByVarietyVarietyIdAndProductionDateBetween(
            Long varietyId, LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = {"variety", "process"})
    @Query("""
            SELECT p FROM Production p
            WHERE p.status = :status
              AND p.productionDate BETWEEN :from AND :to
            ORDER BY p.productionDate ASC, p.id ASC
            """)
    List<Production> findForAnalytics(
            @Param("status") ProductionStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
