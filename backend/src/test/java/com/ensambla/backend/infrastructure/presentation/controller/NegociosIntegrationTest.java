package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class NegociosIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String API = "/api/negocios";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GeocodificadorPort geocodificadorPort;

    private MockMvc mockMvc;
    private UUID usuarioId;
    private UUID negocioInicialId;
    private MockHttpSession sesion;
    private String csrf;

    @BeforeEach
    void setUp() throws Exception {
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenReturn(new LocalidadesDelPunto(List.of("San Justo")));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = givenUsuario();
        negocioInicialId = givenNegocio(usuarioId, "Casa central");
        givenSuscripcion(usuarioId, "PREMIUM");
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Lista, crea, activa y elimina negocios conservando el aislamiento de sus datos")
    void flujoCompletoShouldKeepActiveBusinessIsolated() throws Exception {
        mockMvc.perform(get(API).session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.activoId").value(negocioInicialId.toString()))
                .andExpect(jsonPath("$.limite").value(6))
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.negocios[0].nombre").value("Casa central"))
                .andExpect(jsonPath("$.negocios[0].activo").value(true));

        MvcResult creacion = mockMvc.perform(post(API).session(sesion).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":" Sucursal San Justo ","rubro":"ALMACEN",
                                 "ubicacion":{"direccion":"Arieta 3200","localidad":"San Justo",
                                 "latitud":-34.6701234,"longitud":-58.5607894}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.nombre").value("Sucursal San Justo"))
                .andExpect(jsonPath("$.activo").value(false))
                .andReturn();
        String negocioNuevoId = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");

        assertThat(jdbcTemplate.queryForObject("SELECT direccion FROM negocio WHERE id = ?", String.class,
                UUID.fromString(negocioNuevoId))).isEqualTo("Arieta 3200");

        mockMvc.perform(put(API + "/{id}/activo", negocioNuevoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activoId").value(negocioNuevoId))
                .andExpect(jsonPath("$.negocios[1].activo").value(true));

        mockMvc.perform(get("/api/business").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sucursal San Justo"));

        MvcResult proyecto = mockMvc.perform(post("/api/proyectos").session(sesion)
                        .header("X-CSRF-TOKEN", csrf).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Stock sucursal\"}"))
                .andExpect(status().isCreated()).andReturn();
        String proyectoId = JsonPath.read(proyecto.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put(API + "/{id}/activo", negocioInicialId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/proyectos/{id}", proyectoId).session(sesion))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(API + "/{id}/activo", negocioNuevoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());
        mockMvc.perform(delete(API + "/{id}", negocioNuevoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));

        mockMvc.perform(get(API).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activoId").value(negocioInicialId.toString()))
                .andExpect(jsonPath("$.negocios.length()").value(1));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM proyecto WHERE id = ?", Long.class,
                UUID.fromString(proyectoId))).isZero();
    }

    @Test
    @DisplayName("No permite activar ni eliminar negocios de otra cuenta")
    void mutationsShouldHideForeignBusiness() throws Exception {
        UUID ajenoId = givenNegocio(givenUsuario(), "Ajeno");

        mockMvc.perform(put(API + "/{id}/activo", ajenoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(API + "/{id}", ajenoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isNotFound());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM negocio WHERE id = ?", Long.class, ajenoId))
                .isOne();
    }

    @Test
    @DisplayName("No permite eliminar el único negocio de la cuenta")
    void deleteShouldRejectLastBusiness() throws Exception {
        mockMvc.perform(delete(API + "/{id}", negocioInicialId).session(sesion)
                        .header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No podés eliminar el único negocio de tu cuenta."));
    }

    @Test
    @DisplayName("El plan gratuito rechaza un segundo negocio y expone el código para ir a Planes")
    void createShouldRespectFreePlanLimit() throws Exception {
        jdbcTemplate.update("UPDATE suscripcion SET plan_id = (SELECT id FROM plan WHERE codigo = 'GRATUITO') "
                + "WHERE usuario_id = ?", usuarioId);

        mockMvc.perform(post(API).session(sesion).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otro\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("LIMITE_NEGOCIOS"));
    }

    @Test
    @DisplayName("Las escrituras exigen sesión y protección CSRF")
    void mutationsShouldRequireAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(API).session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\"}"))
                .andExpect(status().isForbidden());
    }

    private UUID givenUsuario() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')",
                id, id + "@example.test");
        return id;
    }

    private UUID givenNegocio(UUID duenioId, String nombre) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, ?, ?)", id, nombre, duenioId);
        return id;
    }

    private void givenSuscripcion(UUID cuentaId, String codigoPlan) {
        jdbcTemplate.update("INSERT INTO suscripcion (usuario_id, plan_id, estado, inicio_en) "
                        + "SELECT ?, id, 'ACTIVA', now() FROM plan WHERE codigo = ?",
                cuentaId, codigoPlan);
    }

    private MockHttpSession givenSesion(UUID cuentaId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(cuentaId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrf = JsonPath.read(body, "$.token");
        return session;
    }
}
