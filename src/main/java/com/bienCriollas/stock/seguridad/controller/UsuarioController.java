package com.bienCriollas.stock.seguridad.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.seguridad.dto.CambiarEstadoUsuarioRequestDTO;
import com.bienCriollas.stock.seguridad.dto.CambiarPasswordRequestDTO;
import com.bienCriollas.stock.seguridad.dto.CambiarRolUsuarioRequestDTO;
import com.bienCriollas.stock.seguridad.dto.UsuarioRequestDTO;
import com.bienCriollas.stock.seguridad.dto.UsuarioResponseDTO;
import com.bienCriollas.stock.seguridad.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Usuarios", description = "Administración de accesos. Requiere rol ADMINISTRADOR.")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping
    @Operation(summary = "Crear un usuario")
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO request) {
        UsuarioResponseDTO usuario = usuarioService.crear(request);
        return ResponseEntity.created(URI.create("/api/v2/usuarios/" + usuario.id())).body(usuario);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o desactivar un usuario", description = "Al desactivarlo se revocan sus tokens.")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CambiarEstadoUsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(
                id, request.activo(), authentication.getName()));
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Cambiar el rol de un usuario", description = "Revoca sus tokens emitidos anteriormente.")
    public ResponseEntity<UsuarioResponseDTO> cambiarRol(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CambiarRolUsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.cambiarRol(
                id, request.rol(), authentication.getName()));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Restablecer la contraseña de un usuario", description = "Revoca sus tokens emitidos anteriormente.")
    public ResponseEntity<Void> restablecerPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequestDTO request) {
        usuarioService.restablecerPassword(id, request.password());
        return ResponseEntity.noContent().build();
    }
}
