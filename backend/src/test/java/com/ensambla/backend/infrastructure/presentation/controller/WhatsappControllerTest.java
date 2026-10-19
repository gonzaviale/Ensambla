package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.GenerarCodigoWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.whatsapp.GenerarCodigoWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.RecibirMensajesWhatsappUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.whatsapp.WhatsappPresentationMapper;
import com.ensambla.backend.infrastructure.whatsapp.LectorWebhookMeta;
import com.ensambla.backend.infrastructure.whatsapp.VerificadorWebhookMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.security.Principal;

import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.APP_SECRET;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.AVISO_AUDIO;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.AVISO_ESTADO;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.PROPERTIES;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.VERIFY_TOKEN;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.bytes;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.firma;
import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsappControllerTest {

    private static final String FIRMA = "X-Hub-Signature-256";

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private GenerarCodigoWhatsappUseCase generarCodigoWhatsappUseCase;
    private RecibirMensajesWhatsappUseCase recibirMensajesWhatsappUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        generarCodigoWhatsappUseCase = mock(GenerarCodigoWhatsappUseCase.class);
        recibirMensajesWhatsappUseCase = mock(RecibirMensajesWhatsappUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WhatsappController(
                        generarCodigoWhatsappUseCase,
                        recibirMensajesWhatsappUseCase,
                        new WhatsappPresentationMapper(),
                        new VerificadorWebhookMeta(PROPERTIES),
                        new LectorWebhookMeta(JsonMapper.builder().build())))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST del código lo genera para el usuario de la sesión y lo devuelve con su vencimiento")
    void generarCodigoShouldReturnCodigoForSessionUser() throws Exception {
        when(generarCodigoWhatsappUseCase.execute(any())).thenReturn(new CodigoWhatsappItem(CODIGO, AHORA));

        ResultActions result = mockMvc.perform(post("/api/whatsapp/vinculo/codigo").principal(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(CODIGO))
                .andExpect(jsonPath("$.venceEn").exists());
        verify(generarCodigoWhatsappUseCase).execute(new GenerarCodigoWhatsappRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("POST del código responde 404 si el usuario no tiene negocio")
    void generarCodigoShouldReturn404WithoutNegocio() throws Exception {
        when(generarCodigoWhatsappUseCase.execute(any())).thenThrow(new ResourceNotFoundException("Sin negocio"));

        ResultActions result = mockMvc.perform(post("/api/whatsapp/vinculo/codigo").principal(sesion));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET del webhook con el token correcto devuelve el challenge de Meta")
    void verificarShouldReturnChallenge() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/whatsapp/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", VERIFY_TOKEN)
                .param("hub.challenge", "1158201444"));

        result.andExpect(status().isOk())
                .andExpect(content().string("1158201444"));
    }

    @Test
    @DisplayName("GET del webhook con otro token responde 403")
    void verificarShouldReturn403WithWrongToken() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/whatsapp/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "otro")
                .param("hub.challenge", "1158201444"));

        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST del webhook firmado pasa los mensajes al use case y responde 200")
    void recibirShouldPassMessagesWhenSigned() throws Exception {
        byte[] cuerpo = bytes(AVISO_AUDIO);

        ResultActions result = mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FIRMA, firma(cuerpo, APP_SECRET))
                .content(cuerpo));

        result.andExpect(status().isOk());
        assertThat(thenReceived().mensajes()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.tipo()).isEqualTo("AUDIO");
            assertThat(mensaje.mediaId()).isEqualTo("1234567890123456");
        });
    }

    @Test
    @DisplayName("POST del webhook sin mensajes (un aviso de entrega) también responde 200")
    void recibirShouldReturn200ForStatusUpdates() throws Exception {
        byte[] cuerpo = bytes(AVISO_ESTADO);

        ResultActions result = mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FIRMA, firma(cuerpo, APP_SECRET))
                .content(cuerpo));

        result.andExpect(status().isOk());
        assertThat(thenReceived().mensajes()).isEmpty();
    }

    @Test
    @DisplayName("POST del webhook con una firma inválida responde 401 y no guarda nada")
    void recibirShouldReturn401WithInvalidSignature() throws Exception {
        byte[] cuerpo = bytes(AVISO_AUDIO);

        ResultActions result = mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FIRMA, firma(cuerpo, "otro-secreto"))
                .content(cuerpo));

        result.andExpect(status().isUnauthorized());
        verifyNoInteractions(recibirMensajesWhatsappUseCase);
    }

    @Test
    @DisplayName("POST del webhook sin firma responde 401 y no guarda nada")
    void recibirShouldReturn401WithoutSignature() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bytes(AVISO_AUDIO)));

        result.andExpect(status().isUnauthorized());
        verifyNoInteractions(recibirMensajesWhatsappUseCase);
    }

    // --- assert ---
    private RecibirMensajesWhatsappRequest thenReceived() {
        ArgumentCaptor<RecibirMensajesWhatsappRequest> captor =
                ArgumentCaptor.forClass(RecibirMensajesWhatsappRequest.class);
        verify(recibirMensajesWhatsappUseCase).execute(captor.capture());
        return captor.getValue();
    }
}
