package com.bienCriollas.stock.security.controller;

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

import com.bienCriollas.stock.security.dto.ChangeUserStatusRequestDTO;
import com.bienCriollas.stock.security.dto.ChangePasswordRequestDTO;
import com.bienCriollas.stock.security.dto.ChangeUserRoleRequestDTO;
import com.bienCriollas.stock.security.dto.UserRequestDTO;
import com.bienCriollas.stock.security.dto.UserResponseDTO;
import com.bienCriollas.stock.security.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Usuarios", description = "Administración de accesos. Requiere rol ADMINISTRADOR.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping
    @Operation(summary = "Crear un usuario")
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO user = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v2/usuarios/" + user.id())).body(user);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o desactivar un usuario", description = "Al desactivarlo se revocan sus tokens.")
    public ResponseEntity<UserResponseDTO> changeStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ChangeUserStatusRequestDTO request) {
        return ResponseEntity.ok(userService.changeStatus(
                id, request.active(), authentication.getName()));
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Cambiar el rol de un usuario", description = "Revoca sus tokens emitidos anteriormente.")
    public ResponseEntity<UserResponseDTO> changeRole(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ChangeUserRoleRequestDTO request) {
        return ResponseEntity.ok(userService.changeRole(
                id, request.role(), authentication.getName()));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Restablecer la contraseña de un usuario", description = "Revoca sus tokens emitidos anteriormente.")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        userService.resetPassword(id, request.password());
        return ResponseEntity.noContent().build();
    }
}
