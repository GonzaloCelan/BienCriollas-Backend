package com.bienCriollas.stock.security.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.security.dto.LoginRequestDTO;
import com.bienCriollas.stock.security.dto.LoginResponseDTO;
import com.bienCriollas.stock.security.dto.UserResponseDTO;
import com.bienCriollas.stock.security.entity.UserAccount;
import com.bienCriollas.stock.security.exception.InvalidCredentialsException;
import com.bienCriollas.stock.security.service.JwtTokenService.IssuedToken;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username().trim(), request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        UserAccount user = userService.findByUsername(request.username());
        IssuedToken token = jwtTokenService.issueToken(user);
        return new LoginResponseDTO(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                UserResponseDTO.from(user));
    }
}
