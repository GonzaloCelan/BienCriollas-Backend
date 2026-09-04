package com.bienCriollas.stock.security.entity;

import java.time.OffsetDateTime;

import com.bienCriollas.stock.security.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    @JsonProperty("idUsuario")
    private Long userId;

    @Column(name = "nombre", nullable = false, length = 100)
    @JsonProperty("nombre")
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    @JsonProperty("rol")
    private UserRole role;

    @Column(name = "activo", nullable = false)
    @JsonProperty("activo")
    private boolean active;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @JsonProperty("creadoEn")
    private OffsetDateTime createdAt;

    @Column(name = "actualizado_en", nullable = false)
    @JsonProperty("actualizadoEn")
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
