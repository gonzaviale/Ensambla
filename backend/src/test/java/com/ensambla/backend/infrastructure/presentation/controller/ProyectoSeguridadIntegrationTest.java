package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
@ActiveProfiles("h2-test")
@Transactional
class ProyectoSeguridadIntegrationTest {

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
    private IProyectoJpaRepository proyectos;

    private MockMvc mockMvc;
    private UUID negocioId;
    private MockHttpSession sesion;

    private String csrfToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuario();
        negocioId = givenNegocio(usuarioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("La lista HTTP contiene sólo proyectos del usuario autenticado")
    void listarShouldIsolateNegocios() throws Exception {
        givenProyecto(negocioId, "Propio");
        givenProyecto(givenNegocio(givenUsuario()), "Ajeno");

        mockMvc.perform(get("/api/proyectos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Propio"));
    }

    @Test
    @DisplayName("Consultar un proyecto ajeno devuelve 404")
    void obtenerShouldHideForeignProyecto() throws Exception {
        UUID ajenoId = givenProyecto(givenNegocio(givenUsuario()), "Ajeno");

        mockMvc.perform(get("/api/proyectos/{id}", ajenoId).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Crear ignora negocioId del body y persiste en el negocio de la sesión")
    void crearShouldUseSessionNegocio() throws Exception {
        UUID ajenoId = givenNegocio(givenUsuario());

        mockMvc.perform(post("/api/proyectos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"nombre\":\" Nuevo \",\"negocioId\":\"" + ajenoId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));

        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId)).hasSize(1);
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(ajenoId)).isEmpty();
    }

    @Test
    @DisplayName("Un PATCH ajeno responde 404 y conserva el nombre original")
    void renombrarShouldNotModifyForeignProyecto() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoId = givenProyecto(ajenoNegocioId, "Ajeno");

        mockMvc.perform(patch("/api/proyectos/{id}", ajenoId).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"Cambio\"}"))
                .andExpect(status().isNotFound());

        assertThat(proyectos.findByIdAndNegocioId(ajenoId, ajenoNegocioId).orElseThrow().getNombre())
                .isEqualTo("Ajeno");
    }

    @Test
    @DisplayName("Un DELETE ajeno responde 404 y conserva el proyecto")
    void eliminarShouldNotDeleteForeignProyecto() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoId = givenProyecto(ajenoNegocioId, "Ajeno");

        mockMvc.perform(delete("/api/proyectos/{id}", ajenoId).session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNotFound());

        assertThat(proyectos.findByIdAndNegocioId(ajenoId, ajenoNegocioId)).isPresent();
    }

    @Test
    @DisplayName("Crear sin CSRF devuelve 403 y no guarda datos")
    void crearShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/proyectos").session(sesion)
                        .contentType("application/json").content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());

        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Crear desde plantilla exige una sesión válida")
    void desdePlantillaShouldRequireSession() throws Exception {
        mockMvc.perform(post("/api/proyectos/desde-plantilla/{id}", UUID.randomUUID())
                        .session(givenSesionSinUsuario()).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized());
    }

    // --- arrange ---
    private UUID givenUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenNegocio(UUID usuarioId) {
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenProyecto(UUID negocioId, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId)
                .nombre(nombre)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private MockHttpSession givenSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = givenSesionSinUsuario();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    private MockHttpSession givenSesionSinUsuario() throws Exception {
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
}


