package com.bienCriollas.stock.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	
	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // para poder hacer POST desde Postman sin lío
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // TODO lo de /stock lo dejamos público
                        .requestMatchers("/stock/**").permitAll()
                        // por ahora, el resto también público
                        .anyRequest().permitAll()
                )
                .build();
    }
}
