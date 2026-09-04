package com.bienCriollas.stock.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.bienCriollas.stock.expense.exception.InvalidExpenseException;
import com.bienCriollas.stock.statistics.exception.InvalidStatisticsRangeException;
import com.bienCriollas.stock.income.exception.InvalidIncomeException;
import com.bienCriollas.stock.waste.exception.InvalidWasteException;
import com.bienCriollas.stock.order.exception.OrderOperationNotAllowedException;
import com.bienCriollas.stock.order.exception.InvalidOrderException;
import com.bienCriollas.stock.order.exception.OrderNotFoundException;
import com.bienCriollas.stock.security.exception.UserOperationNotAllowedException;
import com.bienCriollas.stock.security.exception.InvalidUserException;
import com.bienCriollas.stock.stock.exception.InvalidStockException;
import com.bienCriollas.stock.stock.exception.StockNotFoundException;
import com.bienCriollas.stock.stock.exception.InsufficientStockException;
import com.bienCriollas.stock.security.exception.InvalidCredentialsException;
import com.bienCriollas.stock.security.exception.DuplicateUserException;
import com.bienCriollas.stock.security.exception.UserNotFoundException;
import com.bienCriollas.stock.variety.exception.InactiveVarietyException;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN,
                "No tenés permisos para realizar esta operación", request);
    }

    @ExceptionHandler({
            OrderNotFoundException.class,
            StockNotFoundException.class,
            UserNotFoundException.class,
            VarietyNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            RuntimeException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({
            OrderOperationNotAllowedException.class,
            UserOperationNotAllowedException.class,
            InsufficientStockException.class,
            DuplicateUserException.class,
            InactiveVarietyException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBusinessConflict(
            RuntimeException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("La solicitud contiene datos inválidos");
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
            InvalidExpenseException.class,
            InvalidIncomeException.class,
            InvalidWasteException.class,
            InvalidOrderException.class,
            InvalidStatisticsRangeException.class,
            InvalidStockException.class,
            InvalidUserException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, safeMessage(exception), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        LOGGER.warn("Conflicto de integridad en {}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Error interno en {}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intentá nuevamente.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ARGENTINA_ZONE),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private String safeMessage(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException) {
            return "El cuerpo JSON es inválido o contiene valores incorrectos";
        }
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "La solicitud contiene datos inválidos";
        }
        return exception.getMessage();
    }
}
