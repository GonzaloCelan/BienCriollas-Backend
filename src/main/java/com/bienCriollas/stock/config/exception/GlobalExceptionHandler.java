package com.bienCriollas.stock.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.bienCriollas.stock.pedido.exception.PedidoNoEncontradoException;
import com.bienCriollas.stock.stock.exception.StockNoDisponibleException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> pedidoNoEncontrado(
            PedidoNoEncontradoException exception,
            HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({StockNoDisponibleException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> conflictoDeNegocio(
            RuntimeException exception,
            HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> solicitudInvalida(
            Exception exception,
            HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, mensajeSeguro(exception), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> conflictoDeIntegridad(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        LOGGER.warn("Conflicto de integridad en {}", request.getRequestURI(), exception);
        return respuesta(
                HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> errorInterno(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Error interno en {}", request.getRequestURI(), exception);
        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intentá nuevamente.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> respuesta(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZONA_ARGENTINA),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private String mensajeSeguro(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException) {
            return "El cuerpo JSON es inválido o contiene valores incorrectos";
        }
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "La solicitud contiene datos inválidos";
        }
        return exception.getMessage();
    }
}
