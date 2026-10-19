package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.enums.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.APP_SECRET;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.VERIFY_TOKEN;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.aviso;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.bytes;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.firma;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio de hecho de T-106 de punta a punta, con PostgreSQL, Flyway y la cadena de seguridad reales: un mensaje o una
 * nota de voz de un número vinculado llegan al negocio correcto; un número sin vincular no carga datos en ningún
 * negocio. Cada test hace rollback de la base.
 */
@Transactional
class WhatsappIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final String TELEFONO = "5491122334455";
    private static final String OTRO_TELEFONO = "5491199887766";

    private final JsonMapper json = JsonMapper.builder().build();

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
    private IMensajeWhatsappJpaRepository mensajes;

    private MockMvc mockMvc;
    private int siguienteMensaje;

    @DynamicPropertySource
    static void whatsapp(DynamicPropertyRegistry registry) {
        registry.add("ensambla.whatsapp.verify-token", () -> VERIFY_TOKEN);
        registry.add("ensambla.whatsapp.app-secret", () -> APP_SECRET);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    @Test
    @DisplayName("Meta confirma el webhook sin sesión con el token configurado")
    void verificarShouldBePublicWithTheToken() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/whatsapp/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", VERIFY_TOKEN)
                .param("hub.challenge", "1158201444"));

        result.andExpect(status().isOk())
                .andExpect(content().string("1158201444"));
    }

    @Test
    @DisplayName("El número que manda el código queda vinculado y su texto llega a su negocio")
    void textoShouldReachTheNegocioOfTheLinkedNumber() throws Exception {
        UUID negocioId = givenNumeroVinculado(TELEFONO);

        ResultActions result = recibir(texto(TELEFONO, "Vendí 3 velas a Marta a 4500"));

        result.andExpect(status().isOk());
        MensajeWhatsappEntity mensaje = unicoMensaje();
        assertThat(mensaje.getNegocioId()).isEqualTo(negocioId);
        assertThat(mensaje.getTipo()).isEqualTo(TipoMensajeWhatsapp.TEXTO);
        assertThat(mensaje.getTexto()).isEqualTo("Vendí 3 velas a Marta a 4500");
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.PENDIENTE);
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado llega a su negocio con el id del audio")
    void audioShouldReachTheNegocioOfTheLinkedNumber() throws Exception {
        UUID negocioId = givenNumeroVinculado(TELEFONO);

        recibir(audio(TELEFONO, "998877")).andExpect(status().isOk());

        MensajeWhatsappEntity mensaje = unicoMensaje();
        assertThat(mensaje.getNegocioId()).isEqualTo(negocioId);
        assertThat(mensaje.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
        assertThat(mensaje.getMediaId()).isEqualTo("998877");
    }

    @Test
    @DisplayName("Cada número vinculado carga sólo en su propio negocio")
    void eachNumberShouldReachOnlyItsNegocio() throws Exception {
        UUID negocioA = givenNumeroVinculado(TELEFONO);
        UUID negocioB = givenNumeroVinculado(OTRO_TELEFONO);

        recibir(texto(TELEFONO, "De A")).andExpect(status().isOk());
        recibir(texto(OTRO_TELEFONO, "De B")).andExpect(status().isOk());

        assertThat(mensajes.findAll())
                .extracting(MensajeWhatsappEntity::getTexto, MensajeWhatsappEntity::getNegocioId)
                .containsExactlyInAnyOrder(
                        tuple("De A", negocioA),
                        tuple("De B", negocioB));
    }

    @Test
    @DisplayName("Un número sin vincular no carga datos en ningún negocio, aunque Meta recibe 200")
    void unlinkedNumberShouldNotLoadDataAnywhere() throws Exception {
        givenNumeroVinculado(TELEFONO);

        ResultActions result = recibir(texto(OTRO_TELEFONO, "Vendí 3 velas"));

        result.andExpect(status().isOk());
        assertThat(mensajes.count()).isZero();
    }

    @Test
    @DisplayName("Un código vencido no vincula el número")
    void expiredCodigoShouldNotLink() throws Exception {
        String codigo = givenCodigo(givenSesion(givenNegocio()));
        vinculos.findByCodigo(codigo).orElseThrow().setCodigoVenceEn(AHORA.minusYears(1));
        vinculos.flush();

        recibir(texto(TELEFONO, codigo)).andExpect(status().isOk());

        assertThat(vinculos.findByTelefono(TELEFONO)).isEmpty();
    }

    @Test
    @DisplayName("Un aviso sin firma responde 401 aunque la ruta no pida sesión")
    void recibirShouldRequireTheSignature() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(texto(TELEFONO, "Hola")));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Generar el código sigue pidiendo sesión")
    void generarCodigoShouldRequireSession() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/whatsapp/vinculo/codigo"));

        result.andExpect(status().isUnauthorized());
    }

    // --- arrange ---
    /** El camino completo: la persona pide el código en la app y lo manda desde su WhatsApp. */
    private UUID givenNumeroVinculado(String telefono) throws Exception {
        UUID negocioId = givenNegocio();
        String codigo = givenCodigo(givenSesion(negocioId));
        recibir(texto(telefono, "Ensambla " + codigo)).andExpect(status().isOk());
        assertThat(vinculos.findByTelefono(telefono)).get()
                .extracting(VinculoWhatsappEntity::getNegocioId).isEqualTo(negocioId);
        return negocioId;
    }

    private UUID givenNegocio() {
        UUID usuarioId = usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private Sesion givenSesion(UUID negocioId) throws Exception {
        UUID usuarioId = negocios.findById(negocioId).orElseThrow().getDuenioId();
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        return new Sesion(session, json.readValue(body, CsrfResponse.class).token());
    }

    private String givenCodigo(Sesion sesion) throws Exception {
        String body = mockMvc.perform(post("/api/whatsapp/vinculo/codigo")
                        .session(sesion.session())
                        .header("X-CSRF-TOKEN", sesion.csrfToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("codigo").asString();
    }

    // --- act ---
    private ResultActions recibir(String aviso) throws Exception {
        byte[] cuerpo = bytes(aviso);
        return mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", firma(cuerpo, APP_SECRET))
                .content(cuerpo));
    }

    // --- helpers ---
    private String texto(String telefono, String cuerpo) {
        return aviso("{\"from\":\"" + telefono + "\",\"id\":\"wamid." + siguienteMensaje++ + "\","
                + "\"timestamp\":\"1792411170\",\"type\":\"text\",\"text\":{\"body\":\"" + cuerpo + "\"}}");
    }

    private String audio(String telefono, String mediaId) {
        return aviso("{\"from\":\"" + telefono + "\",\"id\":\"wamid." + siguienteMensaje++ + "\","
                + "\"timestamp\":\"1792411170\",\"type\":\"audio\",\"audio\":{\"id\":\"" + mediaId + "\"}}");
    }

    private MensajeWhatsappEntity unicoMensaje() {
        List<MensajeWhatsappEntity> todos = mensajes.findAll();
        assertThat(todos).hasSize(1);
        return todos.getFirst();
    }

    private record Sesion(MockHttpSession session, String csrfToken) {
    }
}
