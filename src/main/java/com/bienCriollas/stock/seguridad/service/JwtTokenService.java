package com.bienCriollas.stock.seguridad.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.bienCriollas.stock.seguridad.config.SecurityProperties;
import com.bienCriollas.stock.seguridad.entity.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public TokenEmitido emitir(Usuario usuario) {
        Instant emitidoEn = Instant.now();
        Instant expiraEn = emitidoEn.plus(properties.getAccessTokenExpiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .subject(usuario.getUsername())
                .claim("roles", List.of(usuario.getRol().name()))
                .claim("ver", usuario.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenEmitido(token, properties.getAccessTokenExpiration().toSeconds());
    }

    public record TokenEmitido(String valor, long expiraEnSegundos) {
    }
}
