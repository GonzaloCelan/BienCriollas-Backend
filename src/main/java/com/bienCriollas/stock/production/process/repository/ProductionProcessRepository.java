package com.bienCriollas.stock.production.process.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.production.process.entity.ProductionProcess;

import jakarta.persistence.LockModeType;

public interface ProductionProcessRepository extends JpaRepository<ProductionProcess, Long> {

    @EntityGraph(attributePaths = {"variety", "steps"})
    @Query("SELECT DISTINCT p FROM ProductionProcess p WHERE p.id = :id")
    Optional<ProductionProcess> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"variety", "steps"})
    Optional<ProductionProcess> findByVarietyVarietyIdAndActiveTrue(Long varietyId);

    boolean existsByVarietyVarietyIdAndActiveTrue(Long varietyId);

    @EntityGraph(attributePaths = "variety")
    Page<ProductionProcess> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "variety")
    Page<ProductionProcess> findByActiveFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"variety", "steps"})
    List<ProductionProcess> findByVarietyVarietyIdOrderByVersionDesc(Long varietyId);

    Optional<ProductionProcess> findTopByVarietyVarietyIdOrderByVersionDesc(Long varietyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductionProcess p "
            + "WHERE p.variety.varietyId = :varietyId AND p.active = true")
    Optional<ProductionProcess> findActiveByVarietyForUpdate(
            @Param("varietyId") Long varietyId);
}
