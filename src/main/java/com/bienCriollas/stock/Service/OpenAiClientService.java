package com.bienCriollas.stock.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import tools.jackson.databind.JsonNode;

@Service
public class OpenAiClientService {

    private final WebClient webClient;
    private final String model;

    public OpenAiClientService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model}") String model
    ) {
        this.model = model;
        
        System.out.println("OPENAI KEY cargada: " + (apiKey != null ? apiKey.substring(0, 8) + "..." : "NULL"));
        System.out.println("OPENAI MODEL cargado: " + model);

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String analizar(String promptSistema, String promptUsuario) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", promptSistema
                        ),
                        Map.of(
                                "role", "user",
                                "content", promptUsuario
                        )
                )
        );

        JsonNode response = webClient.post()
                .uri("/responses")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            return "No se recibió respuesta de la IA.";
        }

        JsonNode outputText = response.get("output_text");

        if (outputText != null && !outputText.isNull()) {
            return outputText.asText();
        }

        return response.toString();
    }
}