package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
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

@Transactional
class WhatsappSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

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

    private ResultActions respuesta;
    private ResultActions segundaRespuesta;
    private ResultActions terceraRespuesta;

    private String codigo;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = dadoUsuario();
        negocioId = dadoNegocio(usuarioId);
        sesion = dadoSesion(usuarioId);
    }

    @Test
    @DisplayName("Sin sesión no se puede pedir el código ni consultar la vinculación")
    void sinSesionNoSePuedePedirElCodigoNiConsultarLaVinculacion() throws Exception {
        cuandoSeEjecutaSinSesionNoSePuedePedirElCodigoNiConsultarLaVinculacion();

        entoncesSeVerificaSinSesionNoSePuedePedirElCodigoNiConsultarLaVinculacion();
    }

    @Test
    @DisplayName("Vincular con el código del chat y cargar un texto lo guarda en el negocio del usuario")
    void vincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario() throws Exception {
        dadoElEscenarioDeVincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario();

        cuandoSeEjecutaVincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario();

        entoncesSeVerificaVincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario();
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado se guarda como audio")
    void unaNotaDeVozDeUnNumeroVinculadoSeGuardaComoAudio() throws Exception {
        dadoUnNumeroVinculado();

        cuandoLlegaUnaNotaDeVoz();

        entoncesSeGuardaComoAudioDeCuatroSegundos();
    }

    @Test
    @DisplayName("Un número sin vincular no carga datos en ningún negocio y recibe el aviso")
    void unNumeroSinVincularNoCargaDatosEnNingunNegocioYRecibeElAviso() throws Exception {
        cuandoLlegaUnMensajeDeUnNumeroSinVincular();

        entoncesNoSeGuardaNadaYSeAvisaQueNoEstaVinculado();
    }

    @Test
    @DisplayName("El mismo mensaje entregado dos veces se guarda una sola vez")
    void elMismoMensajeEntregadoDosVecesSeGuardaUnaSolaVez() throws Exception {
        dadoUnNumeroVinculado();

        cuandoLlegaDosVecesElMismoMensaje();

        entoncesSeGuardaUnSoloMensaje();
    }

    @Test
    @DisplayName("Un código usado no sirve de nuevo")
    void unCodigoUsadoNoSirveDeNuevo() throws Exception {
        dadoUnCodigoYaUsadoParaVincular();

        cuandoSeVuelveAUsarElCodigo();

        entoncesElNumeroNoQuedaVinculado();
    }

    @Test
    @DisplayName("Desvincular borra el número y el siguiente mensaje se rechaza")
    void desvincularBorraElNumeroYElSiguienteMensajeSeRechaza() throws Exception {
        dadoUnNumeroVinculado();

        cuandoSeDesvinculaYLlegaUnMensaje();

        entoncesElNumeroQuedaDesvinculadoYElMensajeSeRechaza();
    }

    @Test
    @DisplayName("El webhook rechaza con 401 un token incorrecto, aunque no haya sesión ni CSRF")
    void elWebhookRechazaCon401UnTokenIncorrectoAunqueNoHayaSesionNiCSRF() throws Exception {
        cuandoSeEjecutaElWebhookRechazaCon401UnTokenIncorrectoAunqueNoHayaSesionNiCSRF();

        entoncesSeVerificaElWebhookRechazaCon401UnTokenIncorrectoAunqueNoHayaSesionNiCSRF();
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

    private UUID dadoUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID dadoNegocio(UUID duenioId) {
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(duenioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private MockHttpSession dadoSesion(UUID usuario) throws Exception {
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

    // --- dado ---

    private void dadoElEscenarioDeVincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario() throws Exception {
        String codigo = pedirCodigo();
        webhook(mensaje("ID1", "VINCULAR " + codigo)).andExpect(status().isOk());
    }

    private void dadoUnNumeroVinculado() throws Exception {
        webhook(mensaje("ID1", "VINCULAR " + pedirCodigo())).andExpect(status().isOk());
    }

    private void dadoUnCodigoYaUsadoParaVincular() throws Exception {
        codigo = pedirCodigo();
        webhook(mensaje("ID1", "VINCULAR " + codigo)).andExpect(status().isOk());
        vinculos.deleteByUsuarioId(usuarioId);
    }

    // --- cuando ---

    private void cuandoSeEjecutaSinSesionNoSePuedePedirElCodigoNiConsultarLaVinculacion() throws Exception {
        respuesta = mockMvc.perform(post("/api/whatsapp/vinculacion/codigo"));
        segundaRespuesta = mockMvc.perform(get("/api/whatsapp/vinculacion"));
        terceraRespuesta = mockMvc.perform(delete("/api/whatsapp/vinculacion"));
    }

    private void cuandoSeEjecutaVincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario() throws Exception {
        respuesta = mockMvc.perform(get("/api/whatsapp/vinculacion").session(sesion));
    }

    private void cuandoSeEjecutaElWebhookRechazaCon401UnTokenIncorrectoAunqueNoHayaSesionNiCSRF() throws Exception {
        respuesta = mockMvc.perform(post("/api/whatsapp/webhook").header("X-Ensambla-Webhook-Token", "otro")
                        .contentType(MediaType.APPLICATION_JSON).content(mensaje("ID1", "hola")));
    }

    private void cuandoLlegaUnaNotaDeVoz() throws Exception {
        respuesta = webhook("{\"event\":\"messages.upsert\",\"data\":{\"key\":{\"remoteJid\":\"" + TELEFONO
                + "@s.whatsapp.net\",\"fromMe\":false,\"id\":\"ID2\"},\"message\":{\"audioMessage\":"
                + "{\"mimetype\":\"audio/ogg\",\"seconds\":4}}}}");
    }

    private void cuandoLlegaUnMensajeDeUnNumeroSinVincular() throws Exception {
        respuesta = webhook(mensaje("ID1", "gasté 5000 en harina"));
    }

    private void cuandoLlegaDosVecesElMismoMensaje() throws Exception {
        respuesta = webhook(mensaje("ID2", "vendí 3 panes"));
        segundaRespuesta = webhook(mensaje("ID2", "vendí 3 panes"));
    }

    private void cuandoSeVuelveAUsarElCodigo() throws Exception {
        respuesta = webhook(mensaje("ID2", "VINCULAR " + codigo));
    }

    private void cuandoSeDesvinculaYLlegaUnMensaje() throws Exception {
        respuesta = mockMvc.perform(delete("/api/whatsapp/vinculacion").session(sesion)
                .header("X-CSRF-TOKEN", csrfToken));
        segundaRespuesta = webhook(mensaje("ID2", "vendí 3 panes"));
        terceraRespuesta = mockMvc.perform(get("/api/whatsapp/vinculacion").session(sesion));
    }

    // --- entonces ---

    private void entoncesSeVerificaSinSesionNoSePuedePedirElCodigoNiConsultarLaVinculacion() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
        segundaRespuesta
                .andExpect(status().isUnauthorized());
        terceraRespuesta
                .andExpect(status().isUnauthorized());
    }

    private void entoncesSeVerificaVincularConElCodigoDelChatYCargarUnTextoLoGuardaEnElNegocioDelUsuario() throws Exception {
        respuesta
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

    private void entoncesSeVerificaElWebhookRechazaCon401UnTokenIncorrectoAunqueNoHayaSesionNiCSRF() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
        assertThat(mensajes.findAll()).isEmpty();
    }

    private void entoncesSeGuardaComoAudioDeCuatroSegundos() throws Exception {
        respuesta.andExpect(status().isOk());
        assertThat(mensajes.findAll()).singleElement().satisfies(guardado -> {
            assertThat(guardado.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
            assertThat(guardado.getDuracionSeg()).isEqualTo(4);
        });
    }

    private void entoncesNoSeGuardaNadaYSeAvisaQueNoEstaVinculado() throws Exception {
        respuesta.andExpect(status().isOk());
        assertThat(mensajes.findAll()).isEmpty();
        assertThat(respuestas()).last().extracting(MensajeriaWhatsappSimulada.Enviado::texto)
                .asString().contains("no está vinculado");
    }

    private void entoncesSeGuardaUnSoloMensaje() throws Exception {
        respuesta.andExpect(status().isOk());
        segundaRespuesta.andExpect(status().isOk());
        assertThat(mensajes.findAll()).hasSize(1);
    }

    private void entoncesElNumeroNoQuedaVinculado() throws Exception {
        respuesta.andExpect(status().isOk());
        assertThat(vinculos.findByUsuarioId(usuarioId)).isEmpty();
    }

    private void entoncesElNumeroQuedaDesvinculadoYElMensajeSeRechaza() throws Exception {
        respuesta.andExpect(status().isNoContent());
        segundaRespuesta.andExpect(status().isOk());
        assertThat(mensajes.findAll()).isEmpty();
        terceraRespuesta.andExpect(status().isNotFound());
    }
}
