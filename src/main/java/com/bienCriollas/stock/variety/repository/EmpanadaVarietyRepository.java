package com.bienCriollas.stock.variety.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

import jakarta.transaction.Transactional;


public interface EmpanadaVarietyRepository extends JpaRepository<EmpanadaVariety, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM EmpanadaVariety v WHERE v.varietyId = :varietyId")
    Optional<EmpanadaVariety> findByIdForUpdate(@Param("varietyId") Long varietyId);

    Optional<EmpanadaVariety> findById(Long varietyId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EmpanadaVariety v
           SET v.unitPrice = :unitPrice
         WHERE v.varietyId = :varietyId
    """)
    int updateUnitPrice(@Param("varietyId") Long varietyId,
                        @Param("unitPrice") BigDecimal unitPrice);


    Boolean existsByNameIgnoreCase(String name);


    List<EmpanadaVariety> findByActive(int active);


    Optional<EmpanadaVariety> findByName(String name);
}
