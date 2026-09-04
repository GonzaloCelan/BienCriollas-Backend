package com.bienCriollas.stock.security.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bienCriollas.stock.security.entity.UserAccount;
import com.bienCriollas.stock.security.repository.UserRepository;

public class JwtUserValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token", "El token ya no es válido", null);

    private final UserRepository userRepository;

    public JwtUserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        UserAccount user = userRepository.findByUsernameIgnoreCase(jwt.getSubject()).orElse(null);
        Number version = jwt.getClaim("ver");
        if (user == null
                || !user.isActive()
                || version == null
                || user.getTokenVersion() != version.longValue()) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
