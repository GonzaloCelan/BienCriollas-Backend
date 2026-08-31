package com.bienCriollas.stock.seguridad.dto;

import java.time.OffsetDateTime;

import com.bienCriollas.stock.seguridad.entity.Usuario;
import com.bienCriollas.stock.seguridad.enums.RolUsuario;

public record UsuarioResponseDTO(
        Long id,
        String nombre,
        String username,
        RolUsuario rol,
        boolean activo,
        OffsetDateTime creadoEn) {

    public static UsuarioResponseDTO desde(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getUsername(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getCreadoEn());
    }
}
