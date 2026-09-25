package com.bienCriollas.stock.employee.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import com.bienCriollas.stock.employee.entity.EmployeeWorkDay;

import jakarta.persistence.LockModeType;

public interface EmployeeWorkDayRepository
        extends JpaRepository<EmployeeWorkDay, Long>, JpaSpecificationExecutor<EmployeeWorkDay> {

    boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    @EntityGraph(attributePaths = {"employee", "shifts"})
    @Query("SELECT DISTINCT w FROM EmployeeWorkDay w WHERE w.id = :id")
    Optional<EmployeeWorkDay> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM EmployeeWorkDay w WHERE w.id = :id")
    Optional<EmployeeWorkDay> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "employee")
    @Override
    Page<EmployeeWorkDay> findAll(
            @Nullable Specification<EmployeeWorkDay> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "shifts"})
    @Query("""
            SELECT DISTINCT w FROM EmployeeWorkDay w
            WHERE w.workDate BETWEEN :from AND :to
            ORDER BY w.employee.name, w.employee.id, w.workDate
            """)
    List<EmployeeWorkDay> findDetailedBetween(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @EntityGraph(attributePaths = "employee")
    List<EmployeeWorkDay> findAllByEmployeeIdInAndWorkDate(
            Collection<Long> employeeIds, LocalDate workDate);

    @Query("""
            SELECT w.employee.id AS employeeId,
                   w.employee.name AS employeeName,
                   SUM(w.totalWorkedMinutes) AS workedMinutes,
                   SUM(w.totalAmount) AS amount
            FROM EmployeeWorkDay w
            WHERE w.workDate BETWEEN :from AND :to
            GROUP BY w.employee.id, w.employee.name
            ORDER BY w.employee.name, w.employee.id
            """)
    List<EmployeeWorkSummaryProjection> summarizeByEmployee(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT w.employee.id AS employeeId,
                   w.employee.name AS employeeName,
                   SUM(w.totalWorkedMinutes) AS workedMinutes,
                   SUM(w.totalAmount) AS amount
            FROM EmployeeWorkDay w
            WHERE w.employee.id = :employeeId
              AND (:from IS NULL OR w.workDate >= :from)
              AND (:to IS NULL OR w.workDate <= :to)
            GROUP BY w.employee.id, w.employee.name
            """)
    Optional<EmployeeWorkSummaryProjection> summarizeEmployee(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
