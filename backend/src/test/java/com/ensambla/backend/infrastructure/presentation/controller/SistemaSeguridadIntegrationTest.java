package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los endpoints que carga la pantalla al entrar: sistemas, preferencias y zona. */
@Transactional
class SistemaSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String SISTEMA_ID = "e0b5e2c3a-6d8f-4f1e-9a47-3c2d1e0f9a8b";
    private static final UUID SISTEMA_UUID = UUID.fromString("0b5e2c3a-6d8f-4f1e-9a47-3c2d1e0f9a8b");

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

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
    @DisplayName("Un sistema recién guardado vuelve completo en la lista, con ids, columnas y filas")
    void putThenGetShouldRoundTrip() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content(givenBody("Mi almacén")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SISTEMA_UUID.toString()));

        mockMvc.perform(get("/api/sistemas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(SISTEMA_UUID.toString()))
                .andExpect(jsonPath("$[0].name").value("Mi almacén"))
                .andExpect(jsonPath("$[0].template").value("shop"))
                .andExpect(jsonPath("$[0].options.movements").value(true))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].type").value("inventory"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].key").value("name"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[1].key").value("price"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].name").value("Harina"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].price").value(1200))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[1].name").value("Azúcar"));
    }

    @Test
    @DisplayName("Guardar otra vez reemplaza en vez de duplicar")
    void putTwiceShouldReplace() throws Exception {
        for (String nombre : List.of("Primero", "Segundo")) {
            mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                            .contentType("application/json").content(givenBody(nombre)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/sistemas").session(sesion))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Segundo"));
    }

    @Test
    @DisplayName("La lista contiene sólo sistemas del negocio autenticado")
    void getShouldIsolateNegocios() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(givenBody("Propio"))).andExpect(status().isOk());
        MockHttpSession ajena = givenSesion(UsuarioId.de(givenUsuarioConNegocio()));

        mockMvc.perform(get("/api/sistemas").session(ajena))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Guardar un sistema que es de otro negocio responde 404 y no lo modifica")
    void putShouldNotModifyForeignSistema() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(givenBody("Propio"))).andExpect(status().isOk());
        MockHttpSession ajena = givenSesion(UsuarioId.de(givenUsuarioConNegocio()));

        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(ajena).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content(givenBody("Robado")))
                .andExpect(status().isNotFound());

        assertThat(proyectos.findById(SISTEMA_UUID).orElseThrow().getNombre()).isEqualTo("Propio");
        assertThat(proyectos.findById(SISTEMA_UUID).orElseThrow().getNegocioId()).isEqualTo(negocioId.valor());
    }

    @Test
    @DisplayName("Un cuerpo inválido responde 400 y no guarda nada")
    void putShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"name\":\"Sin secciones\",\"sections\":[]}"))
                .andExpect(status().isBadRequest());

        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor())).isEmpty();
    }

    @Test
    @DisplayName("Guardar sin CSRF devuelve 403 y sin sesión 401")
    void putShouldRequireCsrfAndSession() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                        .contentType("application/json").content(givenBody("Mi almacén")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/sistemas")).andExpect(status().isUnauthorized());

        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor())).isEmpty();
    }

    @Test
    @DisplayName("Eliminar borra el sistema y se puede repetir sin error; uno ajeno no se borra")
    void deleteShouldBeIdempotentAndScoped() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(givenBody("Propio"))).andExpect(status().isOk());
        MockHttpSession ajena = givenSesion(UsuarioId.de(givenUsuarioConNegocio()));

        mockMvc.perform(delete("/api/sistemas/{id}", SISTEMA_ID).session(ajena).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());
        assertThat(proyectos.findById(SISTEMA_UUID)).isPresent();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(delete("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken))
                    .andExpect(status().isNoContent());
        }
        assertThat(proyectos.findById(SISTEMA_UUID)).isEmpty();
    }

    @Test
    @DisplayName("Sin preferencias guardadas devuelve las iniciales y un PUT las conserva")
    void preferenciasShouldRoundTrip() throws Exception {
        mockMvc.perform(get("/api/preferencias").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descartadas.length()").value(0))
                .andExpect(jsonPath("$.bienvenidaVista").value(false))
                .andExpect(jsonPath("$.nombre").doesNotExist());

        mockMvc.perform(put("/api/preferencias").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"descartadas\":[\"a1\"],\"grupos\":{\"ZONA\":false},"
                                + "\"bienvenidaVista\":true,\"nombre\":\"Gonza\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/preferencias").session(sesion))
                .andExpect(jsonPath("$.descartadas[0]").value("a1"))
                .andExpect(jsonPath("$.grupos.ZONA").value(false))
                .andExpect(jsonPath("$.bienvenidaVista").value(true))
                .andExpect(jsonPath("$.nombre").value("Gonza"));
    }

    @Test
    @DisplayName("Un negocio sin zona recibe una zona vacía en vez de un error")
    void zonaShouldBeEmptyWhenNegocioHasNone() throws Exception {
        mockMvc.perform(get("/api/zona/agregados").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(""))
                .andExpect(jsonPath("$.comercios").value(0))
                .andExpect(jsonPath("$.productos.length()").value(0));
    }

    @Test
    @DisplayName("Analizar un lugar que no es una zona conocida ni una dirección buscable responde 400")
    void analisisShouldRejectPlaceThatIsNeitherZoneNorAddress() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"lugar\":\"ab\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- arrange ---
    private String givenBody(String nombre) {
        return """
                {"id":"%s","name":"%s","createdAt":"2026-01-10T09:00:00.000Z","updatedAt":"2026-10-21T10:00:00.000Z",
                 "template":"shop","layout":"mosaic","options":{"movements":true,"expenses":false},"demoAlmacen":false,
                 "sections":[{"id":"e1","name":"Vista general","blocks":[
                   {"id":"eb1","type":"inventory","name":"Productos","description":"","color":"orange","width":"half",
                    "fields":[{"key":"name","label":"Producto","type":"text","required":true},
                              {"key":"price","label":"Precio","type":"currency","required":false,"min":0}],
                    "rows":[{"id":"er1","name":"Harina","price":1200},{"id":"er2","name":"Azúcar","price":900}],
                    "content":""}]}]}
                """.formatted(SISTEMA_ID, nombre);
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

    private UUID givenNegocio(UsuarioId usuarioId) {
        UUID creado = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creado), AHORA.minusDays(1));
        return creado;
    }

    private UUID givenUsuarioConNegocio() {
        UsuarioId usuarioId = UsuarioId.de(givenUsuario());
        givenNegocio(usuarioId);
        return usuarioId.valor();
    }

    private MockHttpSession givenSesion(UsuarioId usuarioId) throws Exception {
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
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }
}
