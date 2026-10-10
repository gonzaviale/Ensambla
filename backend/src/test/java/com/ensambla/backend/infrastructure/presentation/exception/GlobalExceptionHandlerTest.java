package com.ensambla.backend.infrastructure.presentation.exception;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/proyectos";
    private static final String SECRET = "password-secreto";
    private static final String CAUSE_SECRET = "secreto";
    private static final String BUSINESS_MESSAGE = "Regla incumplida";

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", PATH);
    }

    @Test
    @DisplayName("Responde 404 con el mensaje de la excepción cuando no se encuentra el recurso")
    void handleNotFoundShouldReturnNotFoundWithMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("ruta", "id", "x");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        thenResponseHasStatusAndMessage(response, 404, exception.getMessage());
    }

    @Test
    @DisplayName("Responde 409 con el mensaje de la excepción cuando el recurso está duplicado")
    void handleDuplicateShouldReturnConflictWithMessage() {
        DuplicateResourceException exception = new DuplicateResourceException("ruta", "slug", "x");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(exception, request);

        thenResponseHasStatusAndMessage(response, 409, exception.getMessage());
    }

    @Test
    @DisplayName("Responde 400 con el mensaje de la regla de negocio incumplida")
    void handleBusinessRuleShouldReturnBadRequestWithMessage() {
        BusinessRuleException exception = new BusinessRuleException(BUSINESS_MESSAGE);

        ResponseEntity<ErrorResponse> response = handler.handleBusinessRule(exception, request);

        thenResponseHasStatusAndMessage(response, 400, BUSINESS_MESSAGE);
    }

    @Test
    @DisplayName("Incluye la ruta, la fecha y un identificador de traza UUID en la respuesta")
    void handleNotFoundShouldIncludePathTimestampAndTraceId() {
        ResourceNotFoundException exception = new ResourceNotFoundException("ruta", "id", "x");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        thenResponseHasPathTimestampAndTraceId(response);
    }

    @Test
    @DisplayName("Responde 400 con el error por campo cuando falla la validación de un campo")
    void handleValidationExceptionsShouldMapFieldErrorByFieldName() {
        MethodArgumentNotValidException exception = validationExceptionWith(
                new FieldError("query", "size", "Debe ser positivo"));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(exception, request);

        thenResponseIsBadRequestWithError(response, "size", "Debe ser positivo");
    }

    @Test
    @DisplayName("Responde 400 con el error por nombre de objeto cuando falla la validación del objeto")
    void handleValidationExceptionsShouldMapObjectErrorByObjectName() {
        MethodArgumentNotValidException exception = validationExceptionWith(
                new ObjectError("query", "Combinación inválida"));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(exception, request);

        thenResponseIsBadRequestWithError(response, "query", "Combinación inválida");
    }

    @Test
    @DisplayName("Responde 400 identificando el parámetro cuando el tipo es inválido")
    void handleTypeMismatchShouldReturnBadRequestWithParameterName() {
        MethodArgumentTypeMismatchException exception = typeMismatchOnRutaId();

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request);

        thenResponseIsBadRequestWithError(response, "rutaId", "El valor no tiene el formato esperado");
    }

    @Test
    @DisplayName("No expone la causa del error cuando el tipo del parámetro es inválido")
    void handleTypeMismatchShouldNotExposeCause() {
        MethodArgumentTypeMismatchException exception = typeMismatchOnRutaId();

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request);

        thenMessageDoesNotContain(response, CAUSE_SECRET);
    }

    @Test
    @DisplayName("Responde 400 identificando el parámetro obligatorio que falta")
    void handleMissingParameterShouldReturnBadRequestWithParameterName() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("nodoId", "UUID");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(exception, request);

        thenResponseIsBadRequestWithError(response, "nodoId", "El parámetro es obligatorio");
    }

    @Test
    @DisplayName("Responde 409 sin exponer detalles de la base cuando se viola la integridad de datos")
    void handleDataIntegrityShouldReturnConflictWithoutDatabaseDetails() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(SECRET);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(exception, request);

        thenResponseHasStatusWithoutSecret(response, 409);
    }

    @Test
    @DisplayName("Responde 500 sin exponer detalles cuando ocurre un error no controlado")
    void handleGenericShouldReturnInternalServerErrorWithoutDetails() {
        IllegalStateException exception = new IllegalStateException(SECRET);

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(exception, request);

        thenResponseHasStatusWithoutSecret(response, 500);
    }

    @Test
    @DisplayName("Responde 401 sin caché cuando las credenciales son inválidas")
    void handleCredentialsShouldReturnUnauthorizedWithoutCache() {
        ResponseEntity<ErrorResponse> response = handler.handleCredentials(new CredencialesInvalidasException(), request);

        thenResponseHasStatusAndMessage(response, 401, "No se pudo autenticar el acceso");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    @DisplayName("Responde 400 genérico cuando el cuerpo JSON no se puede leer")
    void handleUnreadableShouldReturnBadRequestWithoutDetails() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(SECRET, new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(exception, request);

        thenResponseHasStatusWithoutSecret(response, 400);
    }

    @Test
    @DisplayName("Responde 400 identificando el archivo obligatorio que falta en un pedido multipart")
    void handleMissingPartShouldReturnBadRequestWithPartName() {
        MissingServletRequestPartException exception = new MissingServletRequestPartException("fotos");

        ResponseEntity<ErrorResponse> response = handler.handleMissingPart(exception, request);

        thenResponseIsBadRequestWithError(response, "fotos", "El parámetro es obligatorio");
    }

    @Test
    @DisplayName("Responde 400 con los límites de tamaño cuando el archivo subido es demasiado grande")
    void handleMaxUploadSizeShouldReturnBadRequestWithLimits() {
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(10L * 1024 * 1024);

        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(exception, request);

        thenResponseHasStatusAndMessage(response, 400, "Cada foto puede pesar hasta 10 MB y el lote hasta 100 MB");
    }

    // --- arrange ---
    private MethodArgumentNotValidException validationExceptionWith(ObjectError error) {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "query");
        binding.addError(error);
        return new MethodArgumentNotValidException(null, binding);
    }

    private MethodArgumentTypeMismatchException typeMismatchOnRutaId() {
        return new MethodArgumentTypeMismatchException(
                "texto", UUID.class, "rutaId", null, new IllegalArgumentException(CAUSE_SECRET));
    }

    // --- assert ---
    private void thenResponseHasStatusAndMessage(ResponseEntity<ErrorResponse> response, int status, String message) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    private void thenResponseHasPathTimestampAndTraceId(ResponseEntity<ErrorResponse> response) {
        ErrorResponse body = response.getBody();
        assertThat(body.getPath()).isEqualTo(PATH);
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(UUID.fromString(body.getTraceId())).isNotNull();
    }

    private void thenResponseIsBadRequestWithError(ResponseEntity<ErrorResponse> response, String key, String detail) {
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getErrors()).containsEntry(key, detail);
    }

    private void thenMessageDoesNotContain(ResponseEntity<ErrorResponse> response, String secret) {
        assertThat(response.getBody().getMessage()).doesNotContain(secret);
    }

    private void thenResponseHasStatusWithoutSecret(ResponseEntity<ErrorResponse> response, int status) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        thenMessageDoesNotContain(response, SECRET);
    }
}
