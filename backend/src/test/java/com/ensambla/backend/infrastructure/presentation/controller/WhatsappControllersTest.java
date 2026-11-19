package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp.ConfirmarVerificacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp.ConfirmarVerificacionWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp.ConfirmarVerificacionWhatsappResponse;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsappResponse;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacion;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacionRequest;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacionResponse;
import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.ProcesarMensajeEntrante;
import com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp.SolicitarVerificacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp.SolicitarVerificacionWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp.SolicitarVerificacionWhatsappResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.whatsapp.WhatsappPresentationMapper;
import com.ensambla.backend.infrastructure.whatsapp.EvolutionWebhookParser;
import com.ensambla.backend.infrastructure.whatsapp.WhatsappProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsappControllersTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final String TOKEN = "secreto-de-test";
    private static final String MENSAJE = "{\"event\":\"messages.upsert\",\"data\":{\"key\":{\"remoteJid\":"
            + "\"5491122334455@s.whatsapp.net\",\"fromMe\":false,\"id\":\"ID1\"},"
            + "\"message\":{\"conversation\":\"gasté 5000 en harina\"}}}";

    private final SolicitarVerificacionWhatsapp solicitar = mock(SolicitarVerificacionWhatsapp.class);
    private final ConfirmarVerificacionWhatsapp confirmar = mock(ConfirmarVerificacionWhatsapp.class);
    private final GenerarCodigoVinculacion generarCodigo = mock(GenerarCodigoVinculacion.class);
    private final ConsultarVinculacionWhatsapp consultar = mock(ConsultarVinculacionWhatsapp.class);
    private final DesvincularWhatsapp desvincular = mock(DesvincularWhatsapp.class);
    private final ProcesarMensajeEntrante procesar = mock(ProcesarMensajeEntrante.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ResultActions respuesta;
    private ResultActions segundaRespuesta;

    @Test
    @DisplayName("POST codigo devuelve el código, cuándo vence y el número del bot")
    void pOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot() throws Exception {
        dadoElEscenarioDePOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot();

        cuandoSeEjecutaPOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot();

        entoncesSeVerificaPOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot();
    }

    @Test
    @DisplayName("GET devuelve el número vinculado")
    void gETDevuelveElNumeroVinculado() throws Exception {
        dadoElEscenarioDeGETDevuelveElNumeroVinculado();

        cuandoSeEjecutaGETDevuelveElNumeroVinculado();

        entoncesSeVerificaGETDevuelveElNumeroVinculado();
    }

    @Test
    @DisplayName("GET responde 404 si no hay número vinculado")
    void gETResponde404SiNoHayNumeroVinculado() throws Exception {
        dadoElEscenarioDeGETResponde404SiNoHayNumeroVinculado();

        cuandoSeEjecutaGETResponde404SiNoHayNumeroVinculado();

        entoncesSeVerificaGETResponde404SiNoHayNumeroVinculado();
    }

    @Test
    @DisplayName("DELETE desvincula el número y responde 204")
    void dELETEDesvinculaElNumeroYResponde204() throws Exception {
        cuandoSeEjecutaDELETEDesvinculaElNumeroYResponde204();

        entoncesSeVerificaDELETEDesvinculaElNumeroYResponde204();
    }

    @Test
    @DisplayName("El webhook con el token correcto procesa el mensaje y responde 200")
    void elWebhookConElTokenCorrectoProcesaElMensajeYResponde200() throws Exception {
        cuandoSeEjecutaElWebhookConElTokenCorrectoProcesaElMensajeYResponde200();

        entoncesSeVerificaElWebhookConElTokenCorrectoProcesaElMensajeYResponde200();
    }

    @Test
    @DisplayName("POST verificacion manda el código por WhatsApp y devuelve cuándo vence y cuándo se puede reenviar")
    void pOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar() throws Exception {
        dadoElEscenarioDePOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar();

        cuandoSeEjecutaPOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar();

        entoncesSeVerificaPOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar();
    }

    @Test
    @DisplayName("POST verificacion rechaza con 400 un teléfono vacío o con letras")
    void pOSTVerificacionRechazaCon400UnTelefonoVacioOConLetras() throws Exception {
        cuandoSeEjecutaPOSTVerificacionRechazaCon400UnTelefonoVacioOConLetras();

        entoncesSeVerificaPOSTVerificacionRechazaCon400UnTelefonoVacioOConLetras();
    }

    @Test
    @DisplayName("POST verificacion/confirmar vincula el número y devuelve el número del bot")
    void pOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot() throws Exception {
        dadoElEscenarioDePOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot();

        cuandoSeEjecutaPOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot();

        entoncesSeVerificaPOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot();
    }

    @Test
    @DisplayName("POST verificacion/confirmar responde 400 si el código no tiene 6 dígitos o es incorrecto")
    void pOSTVerificacionConfirmarResponde400SiElCodigoNoTiene6DigitosOEsIncorrecto() throws Exception {
        cuandoSeEjecutaPOSTVerificacionConfirmarResponde400SiElCodigoNoTiene6DigitosOEsIncorrecto();

        entoncesSeVerificaPOSTVerificacionConfirmarResponde400SiElCodigoNoTiene6DigitosOEsIncorrecto();
    }

    @Test
    @DisplayName("El webhook acepta el token como parámetro, porque Evolution no manda headers en el webhook global")
    void elWebhookAceptaElTokenComoParametroPorqueEvolutionNoMandaHeadersEnElWebhookGlobal() throws Exception {
        cuandoSeEjecutaElWebhookAceptaElTokenComoParametroPorqueEvolutionNoMandaHeadersEnElWebhookGlobal();

        entoncesSeVerificaElWebhookAceptaElTokenComoParametroPorqueEvolutionNoMandaHeadersEnElWebhookGlobal();
    }

    @Test
    @DisplayName("El webhook responde 200 y no procesa nada si el evento no es un mensaje")
    void elWebhookResponde200YNoProcesaNadaSiElEventoNoEsUnMensaje() throws Exception {
        cuandoSeEjecutaElWebhookResponde200YNoProcesaNadaSiElEventoNoEsUnMensaje();

        entoncesSeVerificaElWebhookResponde200YNoProcesaNadaSiElEventoNoEsUnMensaje();
    }

    @Test
    @DisplayName("El webhook rechaza con 401 un token incorrecto o ausente")
    void elWebhookRechazaCon401UnTokenIncorrectoOAusente() throws Exception {
        cuandoSeEjecutaElWebhookRechazaCon401UnTokenIncorrectoOAusente();

        entoncesSeVerificaElWebhookRechazaCon401UnTokenIncorrectoOAusente();
    }

    @Test
    @DisplayName("Sin secreto configurado el webhook rechaza todo, incluso un token vacío")
    void sinSecretoConfiguradoElWebhookRechazaTodoInclusoUnTokenVacio() throws Exception {
        cuandoSeEjecutaSinSecretoConfiguradoElWebhookRechazaTodoInclusoUnTokenVacio();

        entoncesSeVerificaSinSecretoConfiguradoElWebhookRechazaTodoInclusoUnTokenVacio();
    }

    private MockMvc vinculacion() {
        var controller = new WhatsappVinculacionController(solicitar, confirmar, generarCodigo, consultar, desvincular,
                new WhatsappPresentationMapper());
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private MockMvc webhook(String secreto) {
        var properties = new WhatsappProperties("simulado", "", secreto,
                new WhatsappProperties.Evolution("", "", "", Duration.ofSeconds(1)),
                new WhatsappProperties.Atencion(false, Duration.ofSeconds(1), Duration.ofMinutes(2), 0.6));
        var controller = new WhatsappWebhookController(new EvolutionWebhookParser(Clock.systemUTC()), procesar,
                properties);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // --- dado ---

    private void dadoElEscenarioDePOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot() throws Exception {
        when(generarCodigo.execute(new GenerarCodigoVinculacionRequest(USUARIO_ID)))
                .thenReturn(new GenerarCodigoVinculacionResponse("ABC234", AHORA, "5491100000000"));
    }

    private void dadoElEscenarioDeGETDevuelveElNumeroVinculado() throws Exception {
        when(consultar.execute(new ConsultarVinculacionWhatsappRequest(USUARIO_ID)))
                .thenReturn(new ConsultarVinculacionWhatsappResponse("5491122334455", AHORA, "5491100000000"));
    }

    private void dadoElEscenarioDeGETResponde404SiNoHayNumeroVinculado() throws Exception {
        when(consultar.execute(new ConsultarVinculacionWhatsappRequest(USUARIO_ID))).thenThrow(new ResourceNotFoundException("No hay un número vinculado"));
    }

    private void dadoElEscenarioDePOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar() throws Exception {
        when(solicitar.execute(new SolicitarVerificacionWhatsappRequest(USUARIO_ID, "+54 9 11 2233 4455")))
                .thenReturn(new SolicitarVerificacionWhatsappResponse("5491122334455", AHORA.plusMinutes(10),
                        AHORA.plusMinutes(1)));
    }

    private void dadoElEscenarioDePOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot() throws Exception {
        when(confirmar.execute(new ConfirmarVerificacionWhatsappRequest(USUARIO_ID, "123456")))
                .thenReturn(new ConfirmarVerificacionWhatsappResponse("5491122334455", AHORA, "5491100000000"));
    }

    // --- cuando ---

    private void cuandoSeEjecutaPOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot() throws Exception {
        respuesta = vinculacion().perform(post("/api/whatsapp/vinculacion/codigo").principal(principal));
    }

    private void cuandoSeEjecutaGETDevuelveElNumeroVinculado() throws Exception {
        respuesta = vinculacion().perform(get("/api/whatsapp/vinculacion").principal(principal));
    }

    private void cuandoSeEjecutaGETResponde404SiNoHayNumeroVinculado() throws Exception {
        respuesta = vinculacion().perform(get("/api/whatsapp/vinculacion").principal(principal));
    }

    private void cuandoSeEjecutaDELETEDesvinculaElNumeroYResponde204() throws Exception {
        respuesta = vinculacion().perform(delete("/api/whatsapp/vinculacion").principal(principal));
    }

    private void cuandoSeEjecutaElWebhookConElTokenCorrectoProcesaElMensajeYResponde200() throws Exception {
        respuesta = webhook(TOKEN).perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE));
    }

    private void cuandoSeEjecutaPOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar() throws Exception {
        respuesta = vinculacion().perform(post("/api/whatsapp/verificacion").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"+54 9 11 2233 4455\"}"));
    }

    private void cuandoSeEjecutaPOSTVerificacionRechazaCon400UnTelefonoVacioOConLetras() throws Exception {
        respuesta = vinculacion().perform(post("/api/whatsapp/verificacion").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"abc\"}"));
        segundaRespuesta = vinculacion().perform(post("/api/whatsapp/verificacion").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"\"}"));
    }

    private void cuandoSeEjecutaPOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot() throws Exception {
        respuesta = vinculacion().perform(post("/api/whatsapp/verificacion/confirmar").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"123456\"}"));
    }

    private void cuandoSeEjecutaPOSTVerificacionConfirmarResponde400SiElCodigoNoTiene6DigitosOEsIncorrecto() throws Exception {
        respuesta = vinculacion().perform(post("/api/whatsapp/verificacion/confirmar").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"12\"}"));
        doThrow(new BusinessRuleException("El código no es correcto. Te quedan 4 intentos"))
                .when(confirmar).execute(new ConfirmarVerificacionWhatsappRequest(USUARIO_ID, "654321"));
        segundaRespuesta = vinculacion().perform(post("/api/whatsapp/verificacion/confirmar").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"654321\"}"));
    }

    private void cuandoSeEjecutaElWebhookAceptaElTokenComoParametroPorqueEvolutionNoMandaHeadersEnElWebhookGlobal() throws Exception {
        respuesta = webhook(TOKEN).perform(post("/api/whatsapp/webhook").param("token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE));
        segundaRespuesta = webhook(TOKEN).perform(post("/api/whatsapp/webhook").param("token", "otro")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE));
    }

    private void cuandoSeEjecutaElWebhookResponde200YNoProcesaNadaSiElEventoNoEsUnMensaje() throws Exception {
        respuesta = webhook(TOKEN).perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"event\":\"connection.update\"}"));
    }

    private void cuandoSeEjecutaElWebhookRechazaCon401UnTokenIncorrectoOAusente() throws Exception {
        respuesta = webhook(TOKEN).perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, "otro")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE));
        segundaRespuesta = webhook(TOKEN).perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE));
    }

    private void cuandoSeEjecutaSinSecretoConfiguradoElWebhookRechazaTodoInclusoUnTokenVacio() throws Exception {
        respuesta = webhook("").perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, "")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE));
    }

    // --- entonces ---

    private void entoncesSeVerificaPOSTCodigoDevuelveElCodigoCuandoVenceYElNumeroDelBot() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ABC234"))
                .andExpect(jsonPath("$.numeroBot").value("5491100000000"));
    }

    private void entoncesSeVerificaGETDevuelveElNumeroVinculado() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5491122334455"));
    }

    private void entoncesSeVerificaGETResponde404SiNoHayNumeroVinculado() throws Exception {
        respuesta
                .andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaDELETEDesvinculaElNumeroYResponde204() throws Exception {
        respuesta
                .andExpect(status().isNoContent());
        verify(desvincular).execute(new DesvincularWhatsappRequest(USUARIO_ID));
    }

    private void entoncesSeVerificaElWebhookConElTokenCorrectoProcesaElMensajeYResponde200() throws Exception {
        respuesta
                .andExpect(status().isOk());
        verify(procesar).execute(any(MensajeEntrante.class));
    }

    private void entoncesSeVerificaPOSTVerificacionMandaElCodigoPorWhatsAppYDevuelveCuandoVenceYCuandoSePuedeReenviar() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5491122334455"))
                .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    private void entoncesSeVerificaPOSTVerificacionRechazaCon400UnTelefonoVacioOConLetras() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        segundaRespuesta
                .andExpect(status().isBadRequest());
        verify(solicitar, never()).execute(any());
    }

    private void entoncesSeVerificaPOSTVerificacionConfirmarVinculaElNumeroYDevuelveElNumeroDelBot() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5491122334455"))
                .andExpect(jsonPath("$.numeroBot").value("5491100000000"));
    }

    private void entoncesSeVerificaPOSTVerificacionConfirmarResponde400SiElCodigoNoTiene6DigitosOEsIncorrecto() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        segundaRespuesta
                .andExpect(status().isBadRequest());
    }

    private void entoncesSeVerificaElWebhookAceptaElTokenComoParametroPorqueEvolutionNoMandaHeadersEnElWebhookGlobal() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isUnauthorized());
        verify(procesar).execute(any(MensajeEntrante.class));
    }

    private void entoncesSeVerificaElWebhookResponde200YNoProcesaNadaSiElEventoNoEsUnMensaje() throws Exception {
        respuesta
                .andExpect(status().isOk());
        verify(procesar, never()).execute(any());
    }

    private void entoncesSeVerificaElWebhookRechazaCon401UnTokenIncorrectoOAusente() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
        segundaRespuesta
                .andExpect(status().isUnauthorized());
        verify(procesar, never()).execute(any());
    }

    private void entoncesSeVerificaSinSecretoConfiguradoElWebhookRechazaTodoInclusoUnTokenVacio() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
        verify(procesar, never()).execute(any());
    }
}
