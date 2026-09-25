package com.bienCriollas.stock.employee.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.employee.entity.Employee;

import jakarta.persistence.LockModeType;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByActiveTrueOrderByNameAscIdAsc();

    List<Employee> findByActiveFalseOrderByNameAscIdAsc();

    List<Employee> findAllByOrderByNameAscIdAsc();

    @Query("""
            SELECT e FROM Employee e
            WHERE e.active = true
              AND LOWER(e.name) LIKE LOWER(CONCAT('%', :#{escape(#query)}, '%'))
                  ESCAPE :#{escapeCharacter()}
            ORDER BY e.name, e.id
            """)
    List<Employee> searchActive(@Param("query") String query);

    long countByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id IN :ids ORDER BY e.id")
    List<Employee> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
