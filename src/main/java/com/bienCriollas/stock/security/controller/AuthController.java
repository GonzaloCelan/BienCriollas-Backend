package com.bienCriollas.stock.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.security.dto.ChangeMyPasswordRequestDTO;
import com.bienCriollas.stock.security.dto.LoginRequestDTO;
import com.bienCriollas.stock.security.dto.LoginResponseDTO;
import com.bienCriollas.stock.security.dto.UserResponseDTO;
import com.bienCriollas.stock.security.service.AuthService;
import com.bienCriollas.stock.security.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y cuenta del usuario autenticado.")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un JWT Bearer.")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener mi usuario")
    public ResponseEntity<UserResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(UserResponseDTO.from(
                userService.findByUsername(authentication.getName())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca inmediatamente todos los tokens del usuario.")
    public ResponseEntity<Void> logout(Authentication authentication) {
        userService.revokeTokens(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambiar mi contraseña", description = "Revoca los tokens emitidos anteriormente.")
    public ResponseEntity<Void> changeOwnPassword(
            Authentication authentication,
            @Valid @RequestBody ChangeMyPasswordRequestDTO request) {
        userService.changeOwnPassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
