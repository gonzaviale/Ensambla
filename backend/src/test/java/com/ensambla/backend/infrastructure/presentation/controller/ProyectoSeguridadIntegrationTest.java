package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlanJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
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

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ProyectoSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

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
    private IPlanJpaRepository planes;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private IProyectoJpaRepository proyectos;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private MockHttpSession sesion;

    private String csrfToken;

    private ResultActions respuesta;
    private UUID ajenoNegocioId;
    private UUID ajenoId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        dadoPlanGratuito();
        UsuarioId usuarioId = UsuarioId.de(dadoUsuario());
        negocioId = NegocioId.de(dadoNegocio(usuarioId));
        sesion = dadoSesion(usuarioId);
    }

    @Test
    @DisplayName("La lista HTTP contiene sólo proyectos del usuario autenticado")
    void laListaHTTPContieneSoloProyectosDelUsuarioAutenticado() throws Exception {
        dadoElEscenarioDeLaListaHTTPContieneSoloProyectosDelUsuarioAutenticado();

        cuandoSeEjecutaLaListaHTTPContieneSoloProyectosDelUsuarioAutenticado();

        entoncesSeVerificaLaListaHTTPContieneSoloProyectosDelUsuarioAutenticado();
    }

    @Test
    @DisplayName("Consultar un proyecto ajeno devuelve 404")
    void consultarUnProyectoAjenoDevuelve404() throws Exception {
        dadoElEscenarioDeConsultarUnProyectoAjenoDevuelve404();

        cuandoSeEjecutaConsultarUnProyectoAjenoDevuelve404();

        entoncesSeVerificaConsultarUnProyectoAjenoDevuelve404();
    }

    @Test
    @DisplayName("Crear ignora negocioId del body y persiste en el negocio de la sesión")
    void crearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion() throws Exception {
        dadoElEscenarioDeCrearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion();

        cuandoSeEjecutaCrearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion();

        entoncesSeVerificaCrearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion();
    }

    @Test
    @DisplayName("Un PATCH ajeno responde 404 y conserva el nombre original")
    void unPATCHAjenoResponde404YConservaElNombreOriginal() throws Exception {
        dadoElEscenarioDeUnPATCHAjenoResponde404YConservaElNombreOriginal();

        cuandoSeEjecutaUnPATCHAjenoResponde404YConservaElNombreOriginal();

        entoncesSeVerificaUnPATCHAjenoResponde404YConservaElNombreOriginal();
    }

    @Test
    @DisplayName("Un DELETE ajeno responde 404 y conserva el proyecto")
    void unDELETEAjenoResponde404YConservaElProyecto() throws Exception {
        dadoElEscenarioDeUnDELETEAjenoResponde404YConservaElProyecto();

        cuandoSeEjecutaUnDELETEAjenoResponde404YConservaElProyecto();

        entoncesSeVerificaUnDELETEAjenoResponde404YConservaElProyecto();
    }

    @Test
    @DisplayName("Crear sin CSRF devuelve 403 y no guarda datos")
    void crearSinCSRFDevuelve403YNoGuardaDatos() throws Exception {
        cuandoSeEjecutaCrearSinCSRFDevuelve403YNoGuardaDatos();

        entoncesSeVerificaCrearSinCSRFDevuelve403YNoGuardaDatos();
    }

    @Test
    @DisplayName("Crear desde plantilla exige una sesión válida")
    void crearDesdePlantillaExigeUnaSesionValida() throws Exception {
        cuandoSeEjecutaCrearDesdePlantillaExigeUnaSesionValida();

        entoncesSeVerificaCrearDesdePlantillaExigeUnaSesionValida();
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

    private UUID dadoNegocio(UsuarioId usuarioId) {
        UUID creadoId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creadoId), AHORA.minusDays(1));
        return creadoId;
    }

    private UUID dadoProyecto(NegocioId negocioId, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId.valor())
                .nombre(nombre)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private void dadoPlanGratuito() {
        if (planes.findByCodigo(CodigoPlan.GRATUITO).isEmpty()) {
            planes.saveAndFlush(PlanEntity.builder()
                    .codigo(CodigoPlan.GRATUITO).nombre("Gratuito").configurado(false)
                    .limitesConfigurados(true).limiteProyectos(1).limiteRegistros(1000L)
                    .limitePaginasIaMensuales(10).funciones(List.of())
                    .creadoEn(AHORA).actualizadoEn(AHORA).build());
        }
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

    private void dadoElEscenarioDeLaListaHTTPContieneSoloProyectosDelUsuarioAutenticado() throws Exception {
        dadoProyecto(negocioId, "Propio");
        dadoProyecto(NegocioId.de(dadoNegocio(UsuarioId.de(dadoUsuario()))), "Ajeno");
    }

    private void dadoElEscenarioDeConsultarUnProyectoAjenoDevuelve404() throws Exception {
        ajenoId = dadoProyecto(NegocioId.de(dadoNegocio(UsuarioId.de(dadoUsuario()))), "Ajeno");
    }

    private void dadoElEscenarioDeCrearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion() throws Exception {
        ajenoId = dadoNegocio(UsuarioId.de(dadoUsuario()));
    }

    private void dadoElEscenarioDeUnPATCHAjenoResponde404YConservaElNombreOriginal() throws Exception {
        ajenoNegocioId = dadoNegocio(UsuarioId.de(dadoUsuario()));
        ajenoId = dadoProyecto(NegocioId.de(ajenoNegocioId), "Ajeno");
    }

    private void dadoElEscenarioDeUnDELETEAjenoResponde404YConservaElProyecto() throws Exception {
        ajenoNegocioId = dadoNegocio(UsuarioId.de(dadoUsuario()));
        ajenoId = dadoProyecto(NegocioId.de(ajenoNegocioId), "Ajeno");
    }

    // --- cuando ---

    private void cuandoSeEjecutaLaListaHTTPContieneSoloProyectosDelUsuarioAutenticado() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos").session(sesion));
    }

    private void cuandoSeEjecutaConsultarUnProyectoAjenoDevuelve404() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}", ajenoId).session(sesion));
    }

    private void cuandoSeEjecutaCrearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion() throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"nombre\":\" Nuevo \",\"negocioId\":\"" + ajenoId + "\"}"));
    }

    private void cuandoSeEjecutaUnPATCHAjenoResponde404YConservaElNombreOriginal() throws Exception {
        respuesta = mockMvc.perform(patch("/api/proyectos/{id}", ajenoId).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"Cambio\"}"));
    }

    private void cuandoSeEjecutaUnDELETEAjenoResponde404YConservaElProyecto() throws Exception {
        respuesta = mockMvc.perform(delete("/api/proyectos/{id}", ajenoId).session(sesion).header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeEjecutaCrearSinCSRFDevuelve403YNoGuardaDatos() throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos").session(sesion)
                        .contentType("application/json").content("{\"nombre\":\"Nuevo\"}"));
    }

    private void cuandoSeEjecutaCrearDesdePlantillaExigeUnaSesionValida() throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos/desde-plantilla/{id}", UUID.randomUUID())
                        .session(dadoSesionSinUsuario()).header("X-CSRF-TOKEN", csrfToken));
    }

    // --- entonces ---

    private void entoncesSeVerificaLaListaHTTPContieneSoloProyectosDelUsuarioAutenticado() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Propio"));
    }

    private void entoncesSeVerificaConsultarUnProyectoAjenoDevuelve404() throws Exception {
        respuesta
                .andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaCrearIgnoraNegocioIdDelBodyYPersisteEnElNegocioDeLaSesion() throws Exception {
        respuesta
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor())).hasSize(1);
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(ajenoId)).isEmpty();
    }

    private void entoncesSeVerificaUnPATCHAjenoResponde404YConservaElNombreOriginal() throws Exception {
        respuesta
                .andExpect(status().isNotFound());
        assertThat(proyectos.findByIdAndNegocioId(ajenoId, ajenoNegocioId).orElseThrow().getNombre())
                .isEqualTo("Ajeno");
    }

    private void entoncesSeVerificaUnDELETEAjenoResponde404YConservaElProyecto() throws Exception {
        respuesta
                .andExpect(status().isNotFound());
        assertThat(proyectos.findByIdAndNegocioId(ajenoId, ajenoNegocioId)).isPresent();
    }

    private void entoncesSeVerificaCrearSinCSRFDevuelve403YNoGuardaDatos() throws Exception {
        respuesta
                .andExpect(status().isForbidden());
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor())).isEmpty();
    }

    private void entoncesSeVerificaCrearDesdePlantillaExigeUnaSesionValida() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
    }
}
