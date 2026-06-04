package com.bienCriollas.stock.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.Dto.AnalisisIaRequest;
import com.bienCriollas.stock.Dto.AnalisisIaResponse;
import com.bienCriollas.stock.Service.OpenAiClientService;

@RestController
@RequestMapping("/api/estadisticas")
@CrossOrigin(origins = "*")
public class EstadisticasIaController {

    private final OpenAiClientService openAiClientService;

    public EstadisticasIaController(OpenAiClientService openAiClientService) {
        this.openAiClientService = openAiClientService;
    }

    @PostMapping("/ia")
    public AnalisisIaResponse analizar(@RequestBody AnalisisIaRequest request) {
        String promptSistema = """
                Sos un analista de negocio para una casa de empanadas llamada Bien Criollas.
                Respondé en español argentino, claro y breve.
                """;

        String promptUsuario = """
                Tipo de análisis solicitado: %s
                Fecha: %s

                Esto es una prueba de conexión. Respondé diciendo si la conexión con la IA funciona.
                """.formatted(request.tipoAnalisis(), request.fecha());

        String respuesta = openAiClientService.analizar(promptSistema, promptUsuario);

        return new AnalisisIaResponse(
                "Prueba de IA",
                respuesta,
                List.of()
        );
    }
}