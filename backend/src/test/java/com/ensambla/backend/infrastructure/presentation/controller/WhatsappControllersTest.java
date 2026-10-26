package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.MensajeEntrante;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsappResponse;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacion;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacionRequest;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacionResponse;
import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.ProcesarMensajeEntrante;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.whatsapp.WhatsappPresentationMapper;
import com.ensambla.backend.infrastructure.whatsapp.EvolutionWebhookParser;
import com.ensambla.backend.infrastructure.whatsapp.WhatsappProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
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

    private final GenerarCodigoVinculacion generarCodigo = mock(GenerarCodigoVinculacion.class);
    private final ConsultarVinculacionWhatsapp consultar = mock(ConsultarVinculacionWhatsapp.class);
    private final DesvincularWhatsapp desvincular = mock(DesvincularWhatsapp.class);
    private final ProcesarMensajeEntrante procesar = mock(ProcesarMensajeEntrante.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    @Test
    @DisplayName("POST codigo devuelve el código, cuándo vence y el número del bot")
    void generarCodigoShouldReturnCode() throws Exception {
        when(generarCodigo.execute(new GenerarCodigoVinculacionRequest(USUARIO_ID)))
                .thenReturn(new GenerarCodigoVinculacionResponse("ABC234", AHORA, "5491100000000"));

        vinculacion().perform(post("/api/whatsapp/vinculacion/codigo").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ABC234"))
                .andExpect(jsonPath("$.numeroBot").value("5491100000000"));
    }

    @Test
    @DisplayName("GET devuelve el número vinculado")
    void consultarShouldReturnLink() throws Exception {
        when(consultar.execute(new ConsultarVinculacionWhatsappRequest(USUARIO_ID)))
                .thenReturn(new ConsultarVinculacionWhatsappResponse("5491122334455", AHORA));

        vinculacion().perform(get("/api/whatsapp/vinculacion").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5491122334455"));
    }

    @Test
    @DisplayName("GET responde 404 si no hay número vinculado")
    void consultarShouldReturnNotFoundWithoutLink() throws Exception {
        when(consultar.execute(new ConsultarVinculacionWhatsappRequest(USUARIO_ID))).thenThrow(new ResourceNotFoundException("No hay un número vinculado"));

        vinculacion().perform(get("/api/whatsapp/vinculacion").principal(principal))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE desvincula el número y responde 204")
    void desvincularShouldReturnNoContent() throws Exception {
        vinculacion().perform(delete("/api/whatsapp/vinculacion").principal(principal))
                .andExpect(status().isNoContent());

        verify(desvincular).execute(new DesvincularWhatsappRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("El webhook con el token correcto procesa el mensaje y responde 200")
    void webhookShouldProcessMessageWithValidToken() throws Exception {
        webhook(TOKEN).perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isOk());

        verify(procesar).execute(any(MensajeEntrante.class));
    }

    @Test
    @DisplayName("El webhook responde 200 y no procesa nada si el evento no es un mensaje")
    void webhookShouldIgnoreOtherEventsWithOk() throws Exception {
        webhook(TOKEN).perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"event\":\"connection.update\"}"))
                .andExpect(status().isOk());

        verify(procesar, never()).execute(any());
    }

    @Test
    @DisplayName("El webhook rechaza con 401 un token incorrecto o ausente")
    void webhookShouldRejectWrongOrMissingToken() throws Exception {
        webhook(TOKEN).perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, "otro")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isUnauthorized());
        webhook(TOKEN).perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isUnauthorized());

        verify(procesar, never()).execute(any());
    }

    @Test
    @DisplayName("Sin secreto configurado el webhook rechaza todo, incluso un token vacío")
    void webhookShouldRejectEverythingWithoutConfiguredSecret() throws Exception {
        webhook("").perform(post("/api/whatsapp/webhook").header(WhatsappWebhookController.HEADER_TOKEN, "")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isUnauthorized());

        verify(procesar, never()).execute(any());
    }

    private MockMvc vinculacion() {
        var controller = new WhatsappVinculacionController(generarCodigo, consultar, desvincular,
                new WhatsappPresentationMapper());
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private MockMvc webhook(String secreto) {
        var properties = new WhatsappProperties("simulado", "", secreto, new WhatsappProperties.Evolution("", "", ""));
        var controller = new WhatsappWebhookController(new EvolutionWebhookParser(Clock.systemUTC()), procesar,
                properties);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
}
