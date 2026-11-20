package com.ensambla.backend.infrastructure.presentation.exception;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String RUTA = "/api/proyectos";
    private static final String SECRETO = "password-secreto";
    private static final String SECRETO_DE_LA_CAUSA = "secreto";
    private static final String MENSAJE_DE_NEGOCIO = "Regla incumplida";

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest pedido;
    private ResponseEntity<ErrorResponse> respuesta;
    private ResourceNotFoundException noEncontrado;
    private DuplicateResourceException duplicado;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        pedido = new MockHttpServletRequest("GET", RUTA);
    }

    @Test
    @DisplayName("Responde 404 con el mensaje de la excepción cuando no se encuentra el recurso")
    void respondeNoEncontradoConElMensajeDeLaExcepcion() {
        dadoUnRecursoNoEncontrado();

        cuandoSeManejaElRecursoNoEncontrado();

        entoncesLaRespuestaTieneEstadoYMensaje(404, noEncontrado.getMessage());
    }

    @Test
    @DisplayName("Responde 409 con el mensaje de la excepción cuando el recurso está duplicado")
    void respondeConflictoConElMensajeDeLaExcepcionCuandoElRecursoEstaDuplicado() {
        dadoUnRecursoDuplicado();

        cuandoSeManejaElRecursoDuplicado();

        entoncesLaRespuestaTieneEstadoYMensaje(409, duplicado.getMessage());
    }

    @Test
    @DisplayName("Responde 502 con el mensaje cuando la IA no puede leer la foto")
    void respondeBadGatewayConElMensajeCuandoLaIaNoPuedeLeerLaFoto() {
        cuandoLaIaNoPuedeLeerElCuaderno();

        entoncesLaRespuestaTieneEstadoYMensaje(502, "No pudimos leer el cuaderno");
    }

    @Test
    @DisplayName("Responde 400 con el mensaje de la regla de negocio incumplida")
    void respondeBadRequestConElMensajeDeLaReglaDeNegocioIncumplida() {
        cuandoSeIncumpleUnaReglaDeNegocio();

        entoncesLaRespuestaTieneEstadoYMensaje(400, MENSAJE_DE_NEGOCIO);
    }

    @Test
    @DisplayName("Responde 400 con el mensaje de cada campo cuando un registro no es válido")
    void respondeBadRequestConElErrorDeCadaCampoCuandoUnRegistroNoEsValido() {
        cuandoUnRegistroNoEsValido();

        entoncesLaRespuestaEsBadRequestConElError("campo-1", "Este dato es obligatorio");
        entoncesElMensajeEs("Revisá los datos marcados");
    }

    @Test
    @DisplayName("Responde 400 con el mensaje de cada dato cuando la configuración de un campo no es válida")
    void respondeBadRequestConElErrorDeCadaDatoCuandoLaConfiguracionDeUnCampoNoEsValida() {
        cuandoLaConfiguracionDeUnCampoNoEsValida();

        entoncesLaRespuestaEsBadRequestConElError("opciones", "Agregá al menos una opción");
        entoncesElMensajeEs("Revisá la configuración del campo");
    }

    @Test
    @DisplayName("Incluye la ruta, la fecha y un identificador de traza UUID en la respuesta")
    void incluyeLaRutaLaFechaYUnIdentificadorDeTrazaEnLaRespuesta() {
        dadoUnRecursoNoEncontrado();

        cuandoSeManejaElRecursoNoEncontrado();

        entoncesLaRespuestaTieneRutaFechaYTraza();
    }

    @Test
    @DisplayName("Responde 400 con el error por campo cuando falla la validación de un campo")
    void respondeBadRequestConElErrorPorCampoCuandoFallaLaValidacionDeUnCampo() {
        cuandoFallaLaValidacionDeUnCampo();

        entoncesLaRespuestaEsBadRequestConElError("size", "Debe ser positivo");
    }

    @Test
    @DisplayName("Responde 400 con el error por nombre de objeto cuando falla la validación del objeto")
    void respondeBadRequestConElErrorPorNombreDeObjetoCuandoFallaLaValidacionDelObjeto() {
        cuandoFallaLaValidacionDelObjeto();

        entoncesLaRespuestaEsBadRequestConElError("query", "Combinación inválida");
    }

    @Test
    @DisplayName("Responde 400 identificando el parámetro cuando el tipo es inválido")
    void respondeBadRequestIdentificandoElParametroCuandoElTipoEsInvalido() {
        cuandoElTipoDelParametroEsInvalido();

        entoncesLaRespuestaEsBadRequestConElError("rutaId", "El valor no tiene el formato esperado");
    }

    @Test
    @DisplayName("No expone la causa del error cuando el tipo del parámetro es inválido")
    void noExponeLaCausaDelErrorCuandoElTipoDelParametroEsInvalido() {
        cuandoElTipoDelParametroEsInvalido();

        entoncesElMensajeNoContiene(SECRETO_DE_LA_CAUSA);
    }

    @Test
    @DisplayName("Responde 400 identificando el parámetro obligatorio que falta")
    void respondeBadRequestIdentificandoElParametroObligatorioQueFalta() {
        cuandoFaltaUnParametroObligatorio();

        entoncesLaRespuestaEsBadRequestConElError("nodoId", "El parámetro es obligatorio");
    }

    @Test
    @DisplayName("Responde 409 sin exponer detalles de la base cuando se viola la integridad de datos")
    void respondeConflictoSinExponerDetallesDeLaBaseCuandoSeViolaLaIntegridadDeDatos() {
        cuandoSeViolaLaIntegridadDeDatos();

        entoncesLaRespuestaTieneEstadoYNoExponeElSecreto(409);
    }

    @Test
    @DisplayName("Responde 500 sin exponer detalles cuando ocurre un error no controlado")
    void respondeErrorInternoSinExponerDetallesCuandoOcurreUnErrorNoControlado() {
        cuandoOcurreUnErrorNoControlado();

        entoncesLaRespuestaTieneEstadoYNoExponeElSecreto(500);
    }

    @Test
    @DisplayName("Responde 401 sin caché cuando las credenciales son inválidas")
    void respondeNoAutorizadoSinCacheCuandoLasCredencialesSonInvalidas() {
        cuandoLasCredencialesSonInvalidas();

        entoncesLaRespuestaTieneEstadoYMensaje(401, "No se pudo autenticar el acceso");
        entoncesLaRespuestaNoSeGuardaEnCache();
    }

    @Test
    @DisplayName("Responde 400 genérico cuando el cuerpo JSON no se puede leer")
    void respondeBadRequestGenericoCuandoElCuerpoJsonNoSePuedeLeer() {
        cuandoElCuerpoJsonNoSePuedeLeer();

        entoncesLaRespuestaTieneEstadoYNoExponeElSecreto(400);
    }

    @Test
    @DisplayName("Responde 400 identificando el archivo obligatorio que falta en un pedido multipart")
    void respondeBadRequestIdentificandoElArchivoObligatorioQueFaltaEnUnPedidoMultipart() {
        cuandoFaltaUnArchivoObligatorio();

        entoncesLaRespuestaEsBadRequestConElError("fotos", "El parámetro es obligatorio");
    }

    @Test
    @DisplayName("Responde 400 con los límites de tamaño cuando el archivo subido es demasiado grande")
    void respondeBadRequestConLosLimitesDeTamanioCuandoElArchivoSubidoEsDemasiadoGrande() {
        cuandoElArchivoSubidoEsDemasiadoGrande();

        entoncesLaRespuestaTieneEstadoYMensaje(400, "Cada foto puede pesar hasta 10 MB y el lote hasta 100 MB");
    }

    // --- dado ---

    private void dadoUnRecursoNoEncontrado() {
        noEncontrado = new ResourceNotFoundException("ruta", "id", "x");
    }

    private void dadoUnRecursoDuplicado() {
        duplicado = new DuplicateResourceException("ruta", "slug", "x");
    }

    // --- cuando ---

    private void cuandoSeManejaElRecursoNoEncontrado() {
        respuesta = handler.handleNotFound(noEncontrado, pedido);
    }

    private void cuandoSeManejaElRecursoDuplicado() {
        respuesta = handler.handleDuplicate(duplicado, pedido);
    }

    private void cuandoLaIaNoPuedeLeerElCuaderno() {
        respuesta = handler.handleLecturaCuaderno(new LecturaCuadernoException("No pudimos leer el cuaderno"), pedido);
    }

    private void cuandoSeIncumpleUnaReglaDeNegocio() {
        respuesta = handler.handleBusinessRule(new BusinessRuleException(MENSAJE_DE_NEGOCIO), pedido);
    }

    private void cuandoUnRegistroNoEsValido() {
        respuesta = handler.handleRegistroInvalido(
                new RegistroInvalidoException(Map.of("campo-1", "Este dato es obligatorio")), pedido);
    }

    private void cuandoLaConfiguracionDeUnCampoNoEsValida() {
        respuesta = handler.handleCampoInvalido(
                new CampoInvalidoException(Map.of("opciones", "Agregá al menos una opción")), pedido);
    }

    private void cuandoFallaLaValidacionDeUnCampo() {
        respuesta = handler.handleValidationExceptions(
                unaValidacionFallidaCon(new FieldError("query", "size", "Debe ser positivo")), pedido);
    }

    private void cuandoFallaLaValidacionDelObjeto() {
        respuesta = handler.handleValidationExceptions(
                unaValidacionFallidaCon(new ObjectError("query", "Combinación inválida")), pedido);
    }

    private void cuandoElTipoDelParametroEsInvalido() {
        respuesta = handler.handleTypeMismatch(unTipoInvalidoEnRutaId(), pedido);
    }

    private void cuandoFaltaUnParametroObligatorio() {
        respuesta = handler.handleMissingParameter(
                new MissingServletRequestParameterException("nodoId", "UUID"), pedido);
    }

    private void cuandoSeViolaLaIntegridadDeDatos() {
        respuesta = handler.handleDataIntegrity(new DataIntegrityViolationException(SECRETO), pedido);
    }

    private void cuandoOcurreUnErrorNoControlado() {
        respuesta = handler.handleGeneric(new IllegalStateException(SECRETO), pedido);
    }

    private void cuandoLasCredencialesSonInvalidas() {
        respuesta = handler.handleCredentials(new CredencialesInvalidasException(), pedido);
    }

    private void cuandoElCuerpoJsonNoSePuedeLeer() {
        respuesta = handler.handleUnreadable(
                new HttpMessageNotReadableException(SECRETO, new MockHttpInputMessage(new byte[0])), pedido);
    }

    private void cuandoFaltaUnArchivoObligatorio() {
        respuesta = handler.handleMissingPart(new MissingServletRequestPartException("fotos"), pedido);
    }

    private void cuandoElArchivoSubidoEsDemasiadoGrande() {
        respuesta = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10L * 1024 * 1024), pedido);
    }

    // --- entonces ---

    private void entoncesLaRespuestaTieneEstadoYMensaje(int estado, String mensaje) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(estado);
        entoncesElMensajeEs(mensaje);
    }

    private void entoncesElMensajeEs(String mensaje) {
        assertThat(respuesta.getBody().getMessage()).isEqualTo(mensaje);
    }

    private void entoncesElMensajeNoContiene(String secreto) {
        assertThat(respuesta.getBody().getMessage()).doesNotContain(secreto);
    }

    private void entoncesLaRespuestaTieneRutaFechaYTraza() {
        ErrorResponse cuerpo = respuesta.getBody();
        assertThat(cuerpo.getPath()).isEqualTo(RUTA);
        assertThat(cuerpo.getTimestamp()).isNotNull();
        assertThat(UUID.fromString(cuerpo.getTraceId())).isNotNull();
    }

    private void entoncesLaRespuestaEsBadRequestConElError(String clave, String detalle) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody().getErrors()).containsEntry(clave, detalle);
    }

    private void entoncesLaRespuestaTieneEstadoYNoExponeElSecreto(int estado) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(estado);
        entoncesElMensajeNoContiene(SECRETO);
    }

    private void entoncesLaRespuestaNoSeGuardaEnCache() {
        assertThat(respuesta.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    // --- datos ---

    private MethodArgumentNotValidException unaValidacionFallidaCon(ObjectError error) {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "query");
        binding.addError(error);
        return new MethodArgumentNotValidException(null, binding);
    }

    private MethodArgumentTypeMismatchException unTipoInvalidoEnRutaId() {
        return new MethodArgumentTypeMismatchException(
                "texto", UUID.class, "rutaId", null, new IllegalArgumentException(SECRETO_DE_LA_CAUSA));
    }
}
