package com.bienCriollas.stock.config.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.exception.InvalidOrderException;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void convertsBusinessValidationIntoBadRequest() {
        MockHttpServletRequest request = request("/api/v2/pedido/create");

        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidRequest(
                new InvalidOrderException("El pedido es inválido"),
                request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "El pedido es inválido", request.getRequestURI());
    }

    @Test
    void convertsMissingResourceIntoNotFound() {
        MockHttpServletRequest request = request("/api/v2/catalogo/99");

        ResponseEntity<ApiErrorResponse> response = handler.handleResourceNotFound(
                new VarietyNotFoundException(99L),
                request);

        assertResponse(response, HttpStatus.NOT_FOUND, "No se encontró la variedad con id 99",
                request.getRequestURI());
    }

    @Test
    void convertsDisallowedOperationIntoConflict() {
        MockHttpServletRequest request = request("/api/v2/pedido/actualizar-estado/1/CANCELADO");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessConflict(
                new OrderOperationNotAllowedException("Cambio de estado no permitido"),
                request);

        assertResponse(response, HttpStatus.CONFLICT, "Cambio de estado no permitido",
                request.getRequestURI());
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private void assertResponse(
            ResponseEntity<ApiErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage,
            String expectedPath) {
        assertEquals(expectedStatus, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedStatus.value(), body.status());
        assertEquals(expectedMessage, body.message());
        assertEquals(expectedPath, body.path());
        assertNotNull(body.timestamp());
    }
}
