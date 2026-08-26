package com.bienCriollas.stock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void configuraLosMetadatosDeLaApi() {
        var openApi = new OpenApiConfig().bienCriollasOpenApi();

        assertEquals("Bien Criollas API", openApi.getInfo().getTitle());
        assertEquals("2.0", openApi.getInfo().getVersion());
        assertEquals("Gonzalo Celan", openApi.getInfo().getContact().getName());
        assertEquals(
                "https://github.com/GonzaloCelan/BienCriollas-Backend",
                openApi.getExternalDocs().getUrl());
    }
}
