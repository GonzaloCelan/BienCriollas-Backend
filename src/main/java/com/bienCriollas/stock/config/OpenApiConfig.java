package com.bienCriollas.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bienCriollasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bien Criollas API")
                        .version("2.0")
                        .description(
                                "API REST para la gestión de pedidos, stock, catálogo, ingresos, egresos "
                                        + "y estadísticas de Bien Criollas.")
                        .contact(new Contact()
                                .name("Gonzalo Celan")
                                .url("https://github.com/GonzaloCelan")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositorio del proyecto")
                        .url("https://github.com/GonzaloCelan/BienCriollas-Backend"));
    }
}
