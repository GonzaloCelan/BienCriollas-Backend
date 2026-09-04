package com.bienCriollas.stock.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.security.entity.UserAccount;
import com.bienCriollas.stock.security.enums.UserRole;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRoleAndActiveTrue(UserRole role);
}
