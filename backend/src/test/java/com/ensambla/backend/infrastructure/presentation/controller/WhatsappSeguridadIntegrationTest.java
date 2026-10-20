package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICodigoVinculacionJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.infrastructure.whatsapp.MensajeriaWhatsappSimulada;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2-test")
@Transactional
class WhatsappSeguridadIntegrationTest {

    private static final String TOKEN = "secreto-de-test";
    private static final String TELEFONO = "5491122334455";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IVinculoWhatsappJpaRepository vinculos;

    @Autowired
    private ICodigoVinculacionJpaRepository codigos;

    @Autowired
    private IMensajeWhatsappJpaRepository mensajes;

    @Autowired
    private MensajeriaWhatsappPort mensajeria;

    private MockMvc mockMvc;
    private UUID usuarioId;
    private UUID negocioId;
    private MockHttpSession sesion;
    private String csrfToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = givenUsuario();
        negocioId = givenNegocio(usuarioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Sin sesión no se puede pedir el código ni consultar la vinculación")
    void vinculacionShouldRequireSession() throws Exception {
        mockMvc.perform(post("/api/whatsapp/vinculacion/codigo")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/whatsapp/vinculacion")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/whatsapp/vinculacion")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Vincular con el código del chat y cargar un texto lo guarda en el negocio del usuario")
    void flowShouldLinkNumberAndStoreText() throws Exception {
        String codigo = pedirCodigo();

        webhook(mensaje("ID1", "VINCULAR " + codigo)).andExpect(status().isOk());
        mockMvc.perform(get("/api/whatsapp/vinculacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value(TELEFONO));
        webhook(mensaje("ID2", "gasté 5000 en harina")).andExpect(status().isOk());

        assertThat(mensajes.findAll()).singleElement().satisfies(guardado -> {
            assertThat(guardado.getNegocioId()).isEqualTo(negocioId);
            assertThat(guardado.getTipo()).isEqualTo(TipoMensajeWhatsapp.TEXTO);
            assertThat(guardado.getTexto()).isEqualTo("gasté 5000 en harina");
        });
        assertThat(respuestas()).last().extracting(MensajeriaWhatsappSimulada.Enviado::texto)
                .asString().contains("vinculado");
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado se guarda como audio")
    void flowShouldStoreVoiceNote() throws Exception {
        webhook(mensaje("ID1", "VINCULAR " + pedirCodigo())).andExpect(status().isOk());

        webhook("{\"event\":\"messages.upsert\",\"data\":{\"key\":{\"remoteJid\":\"" + TELEFONO
                + "@s.whatsapp.net\",\"fromMe\":false,\"id\":\"ID2\"},\"message\":{\"audioMessage\":"
                + "{\"mimetype\":\"audio/ogg\",\"seconds\":4}}}}").andExpect(status().isOk());

        assertThat(mensajes.findAll()).singleElement().satisfies(guardado -> {
            assertThat(guardado.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
            assertThat(guardado.getDuracionSeg()).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("Un número sin vincular no carga datos en ningún negocio y recibe el aviso")
    void unlinkedNumberShouldNotStoreAnything() throws Exception {
        webhook(mensaje("ID1", "gasté 5000 en harina")).andExpect(status().isOk());

        assertThat(mensajes.findAll()).isEmpty();
        assertThat(respuestas()).last().extracting(MensajeriaWhatsappSimulada.Enviado::texto)
                .asString().contains("no está vinculado");
    }

    @Test
    @DisplayName("El mismo mensaje entregado dos veces se guarda una sola vez")
    void repeatedDeliveryShouldStoreOnce() throws Exception {
        webhook(mensaje("ID1", "VINCULAR " + pedirCodigo())).andExpect(status().isOk());

        webhook(mensaje("ID2", "vendí 3 panes")).andExpect(status().isOk());
        webhook(mensaje("ID2", "vendí 3 panes")).andExpect(status().isOk());

        assertThat(mensajes.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Un código usado no sirve de nuevo")
    void usedCodeShouldNotWorkTwice() throws Exception {
        String codigo = pedirCodigo();
        webhook(mensaje("ID1", "VINCULAR " + codigo)).andExpect(status().isOk());
        vinculos.deleteByUsuarioId(usuarioId);

        webhook(mensaje("ID2", "VINCULAR " + codigo)).andExpect(status().isOk());

        assertThat(vinculos.findByUsuarioId(usuarioId)).isEmpty();
    }

    @Test
    @DisplayName("Desvincular borra el número y el siguiente mensaje se rechaza")
    void unlinkShouldStopAcceptingMessages() throws Exception {
        webhook(mensaje("ID1", "VINCULAR " + pedirCodigo())).andExpect(status().isOk());

        mockMvc.perform(delete("/api/whatsapp/vinculacion").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());
        webhook(mensaje("ID2", "vendí 3 panes")).andExpect(status().isOk());

        assertThat(mensajes.findAll()).isEmpty();
        mockMvc.perform(get("/api/whatsapp/vinculacion").session(sesion)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El webhook rechaza con 401 un token incorrecto, aunque no haya sesión ni CSRF")
    void webhookShouldRejectWrongToken() throws Exception {
        mockMvc.perform(post("/api/whatsapp/webhook").header("X-Ensambla-Webhook-Token", "otro")
                        .contentType(MediaType.APPLICATION_JSON).content(mensaje("ID1", "hola")))
                .andExpect(status().isUnauthorized());

        assertThat(mensajes.findAll()).isEmpty();
    }

    private String pedirCodigo() throws Exception {
        mockMvc.perform(post("/api/whatsapp/vinculacion/codigo").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
        return codigos.findAll().getFirst().getCodigo();
    }

    private ResultActions webhook(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/whatsapp/webhook").header("X-Ensambla-Webhook-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String mensaje(String id, String texto) {
        return "{\"event\":\"messages.upsert\",\"data\":{\"key\":{\"remoteJid\":\"" + TELEFONO
                + "@s.whatsapp.net\",\"fromMe\":false,\"id\":\"" + id + "\"},\"message\":{\"conversation\":\""
                + texto + "\"}}}";
    }

    private List<MensajeriaWhatsappSimulada.Enviado> respuestas() {
        return ((MensajeriaWhatsappSimulada) mensajeria).enviados();
    }

    private UUID givenUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenNegocio(UUID duenioId) {
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(duenioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private MockHttpSession givenSesion(UUID usuario) throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }
}
