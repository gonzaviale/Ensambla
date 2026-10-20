package com.ensambla.backend.infrastructure.presentation.exception;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServicioUbicacionException.class)
    public ResponseEntity<ErrorResponse> handleUbicacion(ServicioUbicacionException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request, null);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredentials(CredencialesInvalidasException e,
                                                         HttpServletRequest request) {
        ResponseEntity<ErrorResponse> error = buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .body(error.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e,
                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo JSON no es válido", request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request, null);
    }

    @ExceptionHandler(RestriccionPlanException.class)
    public ResponseEntity<ErrorResponse> handleRestriccionPlan(RestriccionPlanException e, HttpServletRequest request) {
        ErrorResponse error = buildResponse(HttpStatus.FORBIDDEN, e.getMessage(), request, null).getBody();
        error.setCodigo(e.getCodigo());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).cacheControl(CacheControl.noStore()).body(error);
    }

    @ExceptionHandler(RegistroInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleRegistroInvalido(RegistroInvalidoException e,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request, e.getErrores());
    }

    @ExceptionHandler(CampoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleCampoInvalido(CampoInvalidoException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request, e.getErrores());
    }

    /** FK inexistente (p. ej. organizacionId que no existe) o restricción única violada en la base. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Violación de integridad en {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Los datos enviados violan una restricción de la base de datos", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación en los datos enviados", request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + e.getName(), request,
                Map.of(e.getName(), "El valor no tiene el formato esperado"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Falta el parámetro: " + e.getParameterName(), request,
                Map.of(e.getParameterName(), "El parámetro es obligatorio"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e,
                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Falta el parámetro: " + e.getRequestPartName(), request,
                Map.of(e.getRequestPartName(), "El parámetro es obligatorio"));
    }

    /** El archivo o el pedido superan {@code spring.servlet.multipart.*}; se corta antes de llegar al dominio. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e,
                                                            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Cada foto puede pesar hasta 10 MB y el lote hasta 100 MB", request, null);
    }

    /** La IA no pudo leer la foto o devolvió algo inválido: el mensaje ya está pensado para la persona. */
    @ExceptionHandler(LecturaCuadernoException.class)
    public ResponseEntity<ErrorResponse> handleLecturaCuaderno(LecturaCuadernoException e,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, e.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno en el servidor", request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request,
                                                        Map<String, String> validationErrors) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .traceId(UUID.randomUUID().toString())
                .errors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}
