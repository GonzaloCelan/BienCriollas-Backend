package com.bienCriollas.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ORÍGENES PERMITIDOS
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");

        // Front viejo
        config.addAllowedOriginPattern("https://biencriollas-front-production.up.railway.app");

        // Front nuevo React
        config.addAllowedOriginPattern("https://biencriollas-frontend-react-production.up.railway.app");

        // MÉTODOS
        config.addAllowedMethod("*");

        // HEADERS
        config.addAllowedHeader("*");

        // COOKIES / CREDENCIALES
        config.setAllowCredentials(true);

        // CACHE PREFLIGHT
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}