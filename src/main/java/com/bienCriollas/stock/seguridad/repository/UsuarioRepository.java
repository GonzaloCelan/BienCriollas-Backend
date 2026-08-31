package com.bienCriollas.stock.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.seguridad.entity.Usuario;
import com.bienCriollas.stock.seguridad.enums.RolUsuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRolAndActivoTrue(RolUsuario rol);
}
