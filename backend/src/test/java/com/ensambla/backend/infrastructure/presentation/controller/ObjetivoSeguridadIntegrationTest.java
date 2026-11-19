package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ObjetivoSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    private static final BigDecimal META = new BigDecimal("400000.00");

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
    private IObjetivoJpaRepository objetivos;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private MockHttpSession sesion;
    private String csrfToken;

    private ResultActions respuesta;
    private ResultActions result;
    private NegocioId ajeno;
    private UUID ajenoId;
    private UUID propio;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UsuarioId usuarioId = UsuarioId.de(dadoUsuario());
        negocioId = dadoNegocio(usuarioId);
        sesion = dadoSesion(usuarioId);
    }

    @Test
    @DisplayName("La lista HTTP contiene sólo los objetivos del negocio de la sesión")
    void laListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion() throws Exception {
        dadoElEscenarioDeLaListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion();

        cuandoSeEjecutaLaListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion();

        entoncesSeVerificaLaListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion();
    }

    @Test
    @DisplayName("Un objetivo creado se ve en el listado siguiente")
    void unObjetivoCreadoSeVeEnElListadoSiguiente() throws Exception {
        cuandoSeEjecutaUnObjetivoCreadoSeVeEnElListadoSiguiente();

        entoncesSeVerificaUnObjetivoCreadoSeVeEnElListadoSiguiente();
    }

    @Test
    @DisplayName("Crear ignora el negocio del body y persiste en el de la sesión")
    void crearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion() throws Exception {
        dadoElEscenarioDeCrearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion();

        cuandoSeEjecutaCrearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion();

        entoncesSeVerificaCrearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion();
    }

    @Test
    @DisplayName("Crear sin CSRF devuelve 403 y no guarda datos")
    void crearSinCSRFDevuelve403YNoGuardaDatos() throws Exception {
        cuandoSeEjecutaCrearSinCSRFDevuelve403YNoGuardaDatos();

        entoncesSeVerificaCrearSinCSRFDevuelve403YNoGuardaDatos();
    }

    @Test
    @DisplayName("Listar sin sesión devuelve 401")
    void listarSinSesionDevuelve401() throws Exception {
        cuandoSeEjecutaListarSinSesionDevuelve401();

        entoncesSeVerificaListarSinSesionDevuelve401();
    }

    @Test
    @DisplayName("Eliminar un objetivo propio devuelve 204 y lo borra")
    void eliminarUnObjetivoPropioDevuelve204YLoBorra() throws Exception {
        dadoElEscenarioDeEliminarUnObjetivoPropioDevuelve204YLoBorra();

        cuandoSeEjecutaEliminarUnObjetivoPropioDevuelve204YLoBorra();

        entoncesSeVerificaEliminarUnObjetivoPropioDevuelve204YLoBorra();
    }

    @Test
    @DisplayName("Un DELETE ajeno responde 404 y conserva el objetivo")
    void unDELETEAjenoResponde404YConservaElObjetivo() throws Exception {
        dadoElEscenarioDeUnDELETEAjenoResponde404YConservaElObjetivo();

        cuandoSeEjecutaUnDELETEAjenoResponde404YConservaElObjetivo();

        entoncesSeVerificaUnDELETEAjenoResponde404YConservaElObjetivo();
    }

    // --- dado ---
    private UUID dadoUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private NegocioId dadoNegocio(UsuarioId usuarioId) {
        return NegocioId.de(negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId());
    }

    private UUID dadoObjetivo(NegocioId negocioId, String texto) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId.valor())
                .texto(texto)
                .meta(META)
                .creadoEn(AHORA)
                .build()).getId();
    }

    private MockHttpSession dadoSesion(UsuarioId usuarioId) throws Exception {
        MockHttpSession session = dadoSesionSinUsuario();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    private MockHttpSession dadoSesionSinUsuario() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        return session;
    }

    // --- dado ---

    private void dadoElEscenarioDeLaListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion() throws Exception {
        dadoObjetivo(negocioId, "Propio");
        dadoObjetivo(dadoNegocio(UsuarioId.de(dadoUsuario())), "Ajeno");
    }

    private void dadoElEscenarioDeCrearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion() throws Exception {
        ajeno = dadoNegocio(UsuarioId.de(dadoUsuario()));
    }

    private void dadoElEscenarioDeEliminarUnObjetivoPropioDevuelve204YLoBorra() throws Exception {
        propio = dadoObjetivo(negocioId, "Propio");
    }

    private void dadoElEscenarioDeUnDELETEAjenoResponde404YConservaElObjetivo() throws Exception {
        ajeno = dadoNegocio(UsuarioId.de(dadoUsuario()));
        ajenoId = dadoObjetivo(ajeno, "Ajeno");
    }

    // --- cuando ---

    private void cuandoSeEjecutaLaListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion() throws Exception {
        result = mockMvc.perform(get("/api/objetivos").session(sesion));
    }

    private void cuandoSeEjecutaUnObjetivoCreadoSeVeEnElListadoSiguiente() throws Exception {
        respuesta = mockMvc.perform(post("/api/objetivos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json")
                .content("{\"texto\":\" Ganar más \",\"meta\":400000}"));
        result = mockMvc.perform(get("/api/objetivos").session(sesion));
    }

    private void cuandoSeEjecutaCrearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion() throws Exception {
        result = mockMvc.perform(post("/api/objetivos").session(sesion)
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json")
                .content("{\"texto\":\"Ganar más\",\"meta\":400000,\"negocioId\":\"" + ajeno + "\"}"));
    }

    private void cuandoSeEjecutaCrearSinCSRFDevuelve403YNoGuardaDatos() throws Exception {
        result = mockMvc.perform(post("/api/objetivos").session(sesion)
                .contentType("application/json")
                .content("{\"texto\":\"Ganar más\",\"meta\":400000}"));
    }

    private void cuandoSeEjecutaListarSinSesionDevuelve401() throws Exception {
        result = mockMvc.perform(get("/api/objetivos").session(dadoSesionSinUsuario()));
    }

    private void cuandoSeEjecutaEliminarUnObjetivoPropioDevuelve204YLoBorra() throws Exception {
        result = mockMvc.perform(
                delete("/api/objetivos/{id}", propio).session(sesion).header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeEjecutaUnDELETEAjenoResponde404YConservaElObjetivo() throws Exception {
        result = mockMvc.perform(
                delete("/api/objetivos/{id}", ajenoId).session(sesion).header("X-CSRF-TOKEN", csrfToken));
    }

    // --- entonces ---

    private void entoncesSeVerificaLaListaHTTPContieneSoloLosObjetivosDelNegocioDeLaSesion() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].texto").value("Propio"));
    }

    private void entoncesSeVerificaUnObjetivoCreadoSeVeEnElListadoSiguiente() throws Exception {
        result.andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].texto").value("Ganar más"))
                .andExpect(jsonPath("$[0].meta").value(400000.0));
    }

    private void entoncesSeVerificaCrearIgnoraElNegocioDelBodyYPersisteEnElDeLaSesion() throws Exception {
        result.andExpect(status().isCreated());
        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId.valor())).hasSize(1);
        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(ajeno.valor())).isEmpty();
    }

    private void entoncesSeVerificaCrearSinCSRFDevuelve403YNoGuardaDatos() throws Exception {
        result.andExpect(status().isForbidden());
        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId.valor())).isEmpty();
    }

    private void entoncesSeVerificaListarSinSesionDevuelve401() throws Exception {
        result.andExpect(status().isUnauthorized());
    }

    private void entoncesSeVerificaEliminarUnObjetivoPropioDevuelve204YLoBorra() throws Exception {
        result.andExpect(status().isNoContent());
        assertThat(objetivos.findByIdAndNegocioId(propio, negocioId.valor())).isEmpty();
    }

    private void entoncesSeVerificaUnDELETEAjenoResponde404YConservaElObjetivo() throws Exception {
        result.andExpect(status().isNotFound());
        assertThat(objetivos.findByIdAndNegocioId(ajenoId, ajeno.valor())).isPresent();
    }
}
