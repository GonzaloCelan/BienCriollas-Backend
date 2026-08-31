package com.bienCriollas.stock.seguridad.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.seguridad.dto.LoginRequestDTO;
import com.bienCriollas.stock.seguridad.dto.LoginResponseDTO;
import com.bienCriollas.stock.seguridad.dto.UsuarioResponseDTO;
import com.bienCriollas.stock.seguridad.entity.Usuario;
import com.bienCriollas.stock.seguridad.exception.CredencialesInvalidasException;
import com.bienCriollas.stock.seguridad.service.JwtTokenService.TokenEmitido;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtTokenService jwtTokenService;

    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username().trim(), request.password()));
        } catch (AuthenticationException exception) {
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usuarioService.buscarPorUsername(request.username());
        TokenEmitido token = jwtTokenService.emitir(usuario);
        return new LoginResponseDTO(
                token.valor(),
                "Bearer",
                token.expiraEnSegundos(),
                UsuarioResponseDTO.desde(usuario));
    }
}
