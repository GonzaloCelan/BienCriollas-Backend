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

        // 🔹 Orígenes permitidos
        config.addAllowedOriginPattern("http://localhost:5173");
        config.addAllowedOriginPattern("https://biencriollas-front-production.up.railway.app");

        // 🔹 Métodos y headers
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        // 🔹 Cookies / Authorization headers
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}