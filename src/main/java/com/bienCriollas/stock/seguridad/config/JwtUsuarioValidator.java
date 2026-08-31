package com.bienCriollas.stock.seguridad.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bienCriollas.stock.seguridad.entity.Usuario;
import com.bienCriollas.stock.seguridad.repository.UsuarioRepository;

public class JwtUsuarioValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error TOKEN_INVALIDO = new OAuth2Error(
            "invalid_token", "El token ya no es válido", null);

    private final UsuarioRepository usuarioRepository;

    public JwtUsuarioValidator(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(jwt.getSubject()).orElse(null);
        Number version = jwt.getClaim("ver");
        if (usuario == null
                || !usuario.isActivo()
                || version == null
                || usuario.getTokenVersion() != version.longValue()) {
            return OAuth2TokenValidatorResult.failure(TOKEN_INVALIDO);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
