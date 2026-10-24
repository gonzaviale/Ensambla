package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
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
class BloqueSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

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

    @Autowired
    private ICampoJpaRepository campos;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private MockHttpSession sesion;
    private String csrfToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UsuarioId usuarioId = UsuarioId.de(givenUsuario());
        negocioId = NegocioId.de(givenNegocio(usuarioId));
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Crear agrega el bloque al final del proyecto propio y lo deja guardado")
    void crearShouldPersistBloqueAtTheEnd() throws Exception {
        ProyectoId proyectoId = ProyectoId.de(givenProyecto(negocioId));
        givenBloque(negocioId, proyectoId, "Productos", 0);

        mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\" Clientes \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Clientes"))
                .andExpect(jsonPath("$.posicion").value(1));

        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId.valor(),
                negocioId.valor()))
                .extracting(BloqueEntity::getNombre)
                .containsExactly("Productos", "Clientes");
    }

    @Test
    @DisplayName("Crear con un nombre que ya existe en el proyecto devuelve 409")
    void crearShouldRejectRepeatedName() throws Exception {
        ProyectoId proyectoId = ProyectoId.de(givenProyecto(negocioId));
        givenBloque(negocioId, proyectoId, "Productos", 0);

        mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"productos\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Crear en un proyecto ajeno devuelve 404 y no guarda nada")
    void crearShouldHideForeignProyecto() throws Exception {
        UUID ajenoNegocioId = givenNegocio(UsuarioId.de(givenUsuario()));
        UUID ajenoProyectoId = givenProyecto(NegocioId.de(ajenoNegocioId));

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
        UUID ajenoNegocioId = givenNegocio(UsuarioId.de(givenUsuario()));
        UUID ajenoProyectoId = givenProyecto(NegocioId.de(ajenoNegocioId));
        givenBloque(NegocioId.de(ajenoNegocioId), ProyectoId.de(ajenoProyectoId), "Secreto", 0);

        mockMvc.perform(get("/api/proyectos/{id}/bloques", ajenoProyectoId).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un PATCH a un bloque ajeno responde 404 y conserva el nombre")
    void renombrarShouldNotModifyForeignBloque() throws Exception {
        UUID ajenoNegocioId = givenNegocio(UsuarioId.de(givenUsuario()));
        UUID ajenoBloqueId = givenBloque(NegocioId
                .de(ajenoNegocioId),
                ProyectoId.de(givenProyecto(NegocioId.de(ajenoNegocioId))), "Ajeno", 0);

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
        UUID ajenoNegocioId = givenNegocio(UsuarioId
            .de(givenUsuario()));
        UUID ajenoBloqueId = givenBloque(NegocioId.de(ajenoNegocioId),
                ProyectoId.de(givenProyecto(NegocioId.de(ajenoNegocioId))), "Ajeno", 0);

        mockMvc.perform(delete("/api/bloques/{id}", ajenoBloqueId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNotFound());

        assertThat(bloques.findByIdAndNegocioId(ajenoBloqueId, ajenoNegocioId)).isPresent();
    }

    @Test
    @DisplayName("Un DELETE a una tabla que otra tabla usa en una conexión responde 400 y conserva la tabla")
    void eliminarShouldRejectBloqueUsedByConexion() throws Exception {
        ProyectoId proyectoId = ProyectoId.de(givenProyecto(negocioId));
        UUID clientes = givenBloque(negocioId, proyectoId, "Clientes", 0);
        UUID pedidos = givenBloque(negocioId, proyectoId, "Pedidos", 1);
        givenConexion(BloqueId.de(pedidos), "Cliente", clientes);

        mockMvc.perform(delete("/api/bloques/{id}", clientes).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La tabla «Clientes» se usa en la columna «Cliente» de «Pedidos»"));

        assertThat(bloques.findByIdAndNegocioId(clientes, negocioId.valor())).isPresent();
    }

    @Test
    @DisplayName("Un DELETE a una tabla que sólo se conecta consigo misma la borra")
    void eliminarShouldDeleteBloqueWithSelfConexion() throws Exception {
        UUID clientes = givenBloque(negocioId, ProyectoId.de(givenProyecto(negocioId)), "Clientes", 0);
        givenConexion(BloqueId.de(clientes), "Recomendado por", clientes);

        mockMvc.perform(delete("/api/bloques/{id}", clientes).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());

        assertThat(bloques.findByIdAndNegocioId(clientes, negocioId.valor())).isEmpty();
    }

    @Test
    @DisplayName("Crear un bloque sin CSRF devuelve 403")
    void crearShouldRequireCsrf() throws Exception {
        ProyectoId proyectoId = ProyectoId.de(givenProyecto(negocioId));

        mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion)
                        .contentType("application/json").content("{\"nombre\":\"Clientes\"}"))
                .andExpect(status().isForbidden());

        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId.valor(),
                negocioId.valor())).isEmpty();
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

    private UUID givenNegocio(UsuarioId usuarioId) {
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenProyecto(NegocioId negocioId) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId.valor())
                .nombre("Inventario")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenBloque(NegocioId negocioId, ProyectoId proyectoId, String nombre, int posicion) {
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId.valor())
                .proyectoId(proyectoId.valor())
                .nombre(nombre)
                .posicion((short) posicion)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private void givenConexion(BloqueId bloqueId, String nombre, UUID relacionadoId) {
        campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId.valor())
                .bloqueId(bloqueId.valor())
                .nombre(nombre)
                .tipo(TipoCampo.RELACION)
                .obligatorio(false)
                .opciones(List.of())
                .bloqueRelacionadoId(relacionadoId)
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build());
    }

    private MockHttpSession givenSesion(UsuarioId usuarioId) throws Exception {
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
