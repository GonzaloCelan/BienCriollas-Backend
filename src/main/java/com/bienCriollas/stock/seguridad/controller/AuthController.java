package com.bienCriollas.stock.seguridad.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.seguridad.dto.CambiarMiPasswordRequestDTO;
import com.bienCriollas.stock.seguridad.dto.LoginRequestDTO;
import com.bienCriollas.stock.seguridad.dto.LoginResponseDTO;
import com.bienCriollas.stock.seguridad.dto.UsuarioResponseDTO;
import com.bienCriollas.stock.seguridad.service.AuthService;
import com.bienCriollas.stock.seguridad.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y cuenta del usuario autenticado.")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un JWT Bearer.")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener mi usuario")
    public ResponseEntity<UsuarioResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(UsuarioResponseDTO.desde(
                usuarioService.buscarPorUsername(authentication.getName())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca inmediatamente todos los tokens del usuario.")
    public ResponseEntity<Void> logout(Authentication authentication) {
        usuarioService.revocarTokens(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambiar mi contraseña", description = "Revoca los tokens emitidos anteriormente.")
    public ResponseEntity<Void> cambiarMiPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarMiPasswordRequestDTO request) {
        usuarioService.cambiarMiPassword(
                authentication.getName(), request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }
}
