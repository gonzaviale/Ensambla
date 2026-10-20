package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class NegocioIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String API = "/api/business";
    private static final String CONFIGURACION = """
            {"nombre":"  Mi almacén  ","rubro":"ALMACEN","comparteDatosZona":false,
             "ubicacion":{"direccion":"Calle 123","localidad":"San Justo",
                          "latitud":-34.6701234,"longitud":-58.5607894}}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // El proveedor externo nunca se consulta en los tests: el mapa reconoce "San Justo" salvo que el test diga otra cosa.
    @MockitoBean
    private GeocodificadorPort geocodificadorPort;

    private MockMvc mockMvc;
    private UUID usuarioId;
    private UUID negocioId;
    private MockHttpSession sesion;
    private String csrf;

    @BeforeEach
    void setUp() throws Exception {
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenReturn(new LocalidadesDelPunto(List.of("San Justo")));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = givenUsuario();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Mi negocio', ?)",
                negocioId, usuarioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Consultar configuración exige sesión")
    void obtenerShouldRequireSession() throws Exception {
        mockMvc.perform(get(API)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actualizar configuración exige sesión")
    void actualizarShouldRequireSession() throws Exception {
        mockMvc.perform(patch(API).contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actualizar configuración exige CSRF")
    void actualizarShouldRequireCsrf() throws Exception {
        mockMvc.perform(patch(API).session(sesion).contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El negocio nuevo se consulta sin ubicación y sin exponer identificadores privados")
    void obtenerShouldReturnOwnConfiguration() throws Exception {
        mockMvc.perform(get(API).session(sesion))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.nombre").value("Mi negocio"))
                .andExpect(jsonPath("$.rubro").value("OTRO"))
                .andExpect(jsonPath("$.ubicacion").isEmpty())
                .andExpect(jsonPath("$.zona").isEmpty())
                .andExpect(jsonPath("$.comparteDatosZona").value(true))
                .andExpect(jsonPath("$.duenioId").doesNotExist())
                .andExpect(jsonPath("$.zonaId").doesNotExist());
    }

    @Test
    @DisplayName("Guarda y recupera nombre, rubro, ubicación y privacidad desde PostgreSQL")
    void actualizarShouldPersistAndReloadConfiguration() throws Exception {
        actualizar(CONFIGURACION).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));

        mockMvc.perform(get(API).session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mi almacén"))
                .andExpect(jsonPath("$.rubro").value("ALMACEN"))
                .andExpect(jsonPath("$.ubicacion.direccion").value("Calle 123"))
                .andExpect(jsonPath("$.ubicacion.localidad").value("San Justo"))
                .andExpect(jsonPath("$.ubicacion.latitud").value(-34.670123))
                .andExpect(jsonPath("$.ubicacion.longitud").value(-58.560789))
                .andExpect(jsonPath("$.comparteDatosZona").value(false));
        assertThat(jdbcTemplate.queryForObject("SELECT direccion FROM negocio WHERE id = ?", String.class, negocioId))
                .isEqualTo("Calle 123");
    }

    @Test
    @DisplayName("Un punto dentro de la zona de San Justo queda asignado a esa zona")
    void actualizarShouldAssignZoneContainingPoint() throws Exception {
        actualizar(CONFIGURACION).andExpect(status().isOk())
                .andExpect(jsonPath("$.zona").value("San Justo - UNLaM"));

        assertThat(jdbcTemplate.queryForObject("SELECT zona_id FROM negocio WHERE id = ?", UUID.class, negocioId))
                .isNotNull();
        mockMvc.perform(get(API).session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.zona").value("San Justo - UNLaM"))
                .andExpect(jsonPath("$.zonaId").doesNotExist());
    }

    @Test
    @DisplayName("Un punto fuera de toda zona deja el negocio sin zona")
    void actualizarShouldLeaveZoneEmptyForFarPoint() throws Exception {
        actualizar(CONFIGURACION).andExpect(status().isOk());
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenReturn(new LocalidadesDelPunto(List.of("CABA")));

        actualizar("""
                {"ubicacion":{"direccion":"Av. Corrientes 1000","localidad":"CABA",
                              "latitud":-34.6037,"longitud":-58.3816}}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.zona").isEmpty());

        assertThat(jdbcTemplate.queryForObject("SELECT zona_id FROM negocio WHERE id = ?", UUID.class, negocioId))
                .isNull();
    }

    @Test
    @DisplayName("Una localidad distinta a la del mapa responde 400 y no modifica la ubicación guardada")
    void actualizarShouldRejectLocalityOutsideMap() throws Exception {
        actualizar(CONFIGURACION.replace("\"localidad\":\"San Justo\"", "\"localidad\":\"Ramos Mejía\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La localidad no coincide con la del mapa (San Justo)"));

        assertThat(jdbcTemplate.queryForObject("SELECT localidad FROM negocio WHERE id = ?", String.class, negocioId))
                .isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT nombre FROM negocio WHERE id = ?", String.class, negocioId))
                .isEqualTo("Mi negocio");
    }

    @Test
    @DisplayName("La localidad escrita se acepta sin importar mayúsculas ni tildes")
    void actualizarShouldAcceptLocalityWithDifferentCase() throws Exception {
        actualizar(CONFIGURACION.replace("\"localidad\":\"San Justo\"", "\"localidad\":\"  san JUSTO \""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ubicacion.localidad").value("san JUSTO"));
    }

    @Test
    @DisplayName("Si el proveedor del mapa no responde, responde 503 y no guarda la ubicación")
    void actualizarShouldReturn503WhenMapProviderFails() throws Exception {
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenThrow(new ServicioUbicacionException("No pudimos verificar la localidad."));

        actualizar(CONFIGURACION).andExpect(status().isServiceUnavailable());

        assertThat(jdbcTemplate.queryForObject("SELECT direccion FROM negocio WHERE id = ?", String.class, negocioId))
                .isNull();
    }

    @Test
    @DisplayName("Guardar sin tocar la ubicación no consulta al proveedor del mapa")
    void actualizarShouldNotCallMapProviderForUnchangedLocation() throws Exception {
        actualizar(CONFIGURACION).andExpect(status().isOk());
        clearInvocations(geocodificadorPort);

        actualizar(CONFIGURACION.replace("Mi almacén", "Otro nombre")).andExpect(status().isOk());

        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("Un PATCH parcial conserva ubicación, rubro y privacidad ya guardados")
    void actualizarShouldPreserveFieldsNotSent() throws Exception {
        actualizar(CONFIGURACION).andExpect(status().isOk());

        actualizar("{\"nombre\":\"Otro nombre\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Otro nombre"))
                .andExpect(jsonPath("$.rubro").value("ALMACEN"))
                .andExpect(jsonPath("$.ubicacion.localidad").value("San Justo"))
                .andExpect(jsonPath("$.comparteDatosZona").value(false));
    }

    @Test
    @DisplayName("Los ids enviados por el cliente nunca cambian el negocio de otro dueño")
    void actualizarShouldIgnoreForeignBusinessAndOwnerIds() throws Exception {
        UUID ajeno = UUID.randomUUID();
        UUID otroUsuario = givenUsuario();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Ajeno', ?)", ajeno, otroUsuario);

        actualizar("{\"nombre\":\"Propio\",\"negocioId\":\"" + ajeno + "\",\"duenioId\":\"" + otroUsuario + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Propio"));

        assertThat(jdbcTemplate.queryForObject("SELECT nombre FROM negocio WHERE id = ?", String.class, ajeno))
                .isEqualTo("Ajeno");
        assertThat(jdbcTemplate.queryForObject("SELECT duenio_id FROM negocio WHERE id = ?", UUID.class, negocioId))
                .isEqualTo(usuarioId);
    }

    @Test
    @DisplayName("No encuentra configuración para un usuario sin negocio propio")
    void obtenerShouldRejectUserWithoutBusiness() throws Exception {
        MockHttpSession sinNegocio = givenSesion(givenUsuario());

        mockMvc.perform(get(API).session(sinNegocio)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un usuario sin negocio propio no puede editar otro negocio")
    void actualizarShouldRejectUserWithoutBusiness() throws Exception {
        sesion = givenSesion(givenUsuario());

        actualizar(CONFIGURACION).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nombre\":\"   \"}",
            "{\"rubro\":\"INEXISTENTE\"}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"San Justo\",\"latitud\":91,\"longitud\":0}}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"San Justo\",\"latitud\":0,\"longitud\":181}}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"San Justo\",\"latitud\":0}}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"\",\"latitud\":0,\"longitud\":0}}",
            "{\"ubicacion\":{\"direccion\":\"\",\"localidad\":\"San Justo\",\"latitud\":0,\"longitud\":0}}"
    })
    @DisplayName("Los datos inválidos responden 400 y no modifican el negocio")
    void actualizarShouldRejectInvalidData(String cuerpo) throws Exception {
        actualizar(cuerpo).andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("SELECT nombre FROM negocio WHERE id = ?", String.class, negocioId))
                .isEqualTo("Mi negocio");
    }

    // --- arrange ---
    private UUID givenUsuario() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')", id, id + "@example.test");
        return id;
    }

    private MockHttpSession givenSesion(UUID usuario) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.toString(), null, List.of()));
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

    // --- act ---
    private ResultActions actualizar(String cuerpo) throws Exception {
        return mockMvc.perform(patch(API).session(sesion).header("X-CSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
