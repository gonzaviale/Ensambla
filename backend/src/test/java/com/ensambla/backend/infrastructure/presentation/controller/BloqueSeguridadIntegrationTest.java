package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
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
class BloqueSeguridadIntegrationTest {

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

    @Autowired
    private IBloqueJpaRepository bloques;

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
    @DisplayName("Crear agrega el bloque al final del proyecto propio y lo deja guardado")
    void crearShouldPersistBloqueAtTheEnd() throws Exception {
        UUID proyectoId = givenProyecto(negocioId);
        givenBloque(negocioId, proyectoId, "Productos", 0);

        mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\" Clientes \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Clientes"))
                .andExpect(jsonPath("$.posicion").value(1));

        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId, negocioId))
                .extracting(BloqueEntity::getNombre)
                .containsExactly("Productos", "Clientes");
    }

    @Test
    @DisplayName("Crear con un nombre que ya existe en el proyecto devuelve 409")
    void crearShouldRejectRepeatedName() throws Exception {
        UUID proyectoId = givenProyecto(negocioId);
        givenBloque(negocioId, proyectoId, "Productos", 0);

        mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"productos\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Crear en un proyecto ajeno devuelve 404 y no guarda nada")
    void crearShouldHideForeignProyecto() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoProyectoId = givenProyecto(ajenoNegocioId);

        mockMvc.perform(post("/api/proyectos/{id}/bloques", ajenoProyectoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"Intruso\"}"))
                .andExpect(status().isNotFound());

        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(ajenoProyectoId, ajenoNegocioId))
                .isEmpty();
    }

    @Test
    @DisplayName("Listar los bloques de un proyecto ajeno devuelve 404")
    void listarShouldHideForeignProyecto() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoProyectoId = givenProyecto(ajenoNegocioId);
        givenBloque(ajenoNegocioId, ajenoProyectoId, "Secreto", 0);

        mockMvc.perform(get("/api/proyectos/{id}/bloques", ajenoProyectoId).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un PATCH a un bloque ajeno responde 404 y conserva el nombre")
    void renombrarShouldNotModifyForeignBloque() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoBloqueId = givenBloque(ajenoNegocioId, givenProyecto(ajenoNegocioId), "Ajeno", 0);

        mockMvc.perform(patch("/api/bloques/{id}", ajenoBloqueId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"Cambio\"}"))
                .andExpect(status().isNotFound());

        assertThat(bloques.findByIdAndNegocioId(ajenoBloqueId, ajenoNegocioId).orElseThrow().getNombre())
                .isEqualTo("Ajeno");
    }

    @Test
    @DisplayName("Un DELETE a un bloque ajeno responde 404 y conserva el bloque")
    void eliminarShouldNotDeleteForeignBloque() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoBloqueId = givenBloque(ajenoNegocioId, givenProyecto(ajenoNegocioId), "Ajeno", 0);

        mockMvc.perform(delete("/api/bloques/{id}", ajenoBloqueId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNotFound());

        assertThat(bloques.findByIdAndNegocioId(ajenoBloqueId, ajenoNegocioId)).isPresent();
    }

    @Test
    @DisplayName("Crear un bloque sin CSRF devuelve 403")
    void crearShouldRequireCsrf() throws Exception {
        UUID proyectoId = givenProyecto(negocioId);

        mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion)
                        .contentType("application/json").content("{\"nombre\":\"Clientes\"}"))
                .andExpect(status().isForbidden());

        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId, negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Listar bloques exige una sesión válida")
    void listarShouldRequireSession() throws Exception {
        mockMvc.perform(get("/api/proyectos/{id}/bloques", UUID.randomUUID()).session(givenSesionSinUsuario()))
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

    private UUID givenProyecto(UUID negocioId) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId)
                .nombre("Inventario")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenBloque(UUID negocioId, UUID proyectoId, String nombre, int posicion) {
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre)
                .posicion((short) posicion)
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
