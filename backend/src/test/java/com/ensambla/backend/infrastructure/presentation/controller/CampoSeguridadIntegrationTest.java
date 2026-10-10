package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.enums.TipoCampo;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2-test")
@Transactional
class CampoSeguridadIntegrationTest {

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
    @DisplayName("Crear agrega el campo normalizado al final del bloque propio y lo deja guardado")
    void crearShouldPersistCampoAtTheEnd() throws Exception {
        UUID bloqueId = givenBloque(negocioId);
        givenCampo(negocioId, bloqueId, "Producto", 0);

        crear(bloqueId, """
                {"nombre":" Talle ","tipo":"OPCION","valorInicial":"Chico","opciones":[" Chico ","Grande"]}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.bloqueId").value(bloqueId.toString()))
                .andExpect(jsonPath("$.nombre").value("Talle"))
                .andExpect(jsonPath("$.tipo").value("OPCION"))
                .andExpect(jsonPath("$.obligatorio").value(false))
                .andExpect(jsonPath("$.valorInicial").value("Chico"))
                .andExpect(jsonPath("$.opciones[0]").value("Chico"))
                .andExpect(jsonPath("$.posicion").value(1));

        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId))
                .extracting(CampoEntity::getNombre)
                .containsExactly("Producto", "Talle");
    }

    @Test
    @DisplayName("Listar devuelve los campos del bloque propio ordenados por posición")
    void listarShouldReturnCamposInOrder() throws Exception {
        UUID bloqueId = givenBloque(negocioId);
        givenCampo(negocioId, bloqueId, "Segundo", 1);
        givenCampo(negocioId, bloqueId, "Primero", 0);

        mockMvc.perform(get("/api/bloques/{id}/campos", bloqueId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Primero"))
                .andExpect(jsonPath("$[1].nombre").value("Segundo"))
                .andExpect(jsonPath("$[1].opciones").isArray());
    }

    @Test
    @DisplayName("Un tipo fuera de la lista se rechaza con 400 y no guarda nada")
    void crearShouldRejectUnknownTipo() throws Exception {
        UUID bloqueId = givenBloque(negocioId);

        crear(bloqueId, "{\"nombre\":\"Moneda\",\"tipo\":\"MONEDA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tipo").exists());

        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Una opción sin lista de opciones se rechaza con 400")
    void crearShouldRejectOpcionWithoutOpciones() throws Exception {
        UUID bloqueId = givenBloque(negocioId);

        crear(bloqueId, "{\"nombre\":\"Talle\",\"tipo\":\"OPCION\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.opciones").exists());
    }

    @Test
    @DisplayName("Crear con un nombre que ya existe en el bloque devuelve 409")
    void crearShouldRejectRepeatedName() throws Exception {
        UUID bloqueId = givenBloque(negocioId);
        givenCampo(negocioId, bloqueId, "Producto", 0);

        crear(bloqueId, "{\"nombre\":\" PRODUCTO \",\"tipo\":\"TEXTO\"}")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Editar reemplaza la configuración del campo propio")
    void editarShouldReplaceConfiguracion() throws Exception {
        UUID campoId = givenCampo(negocioId, givenBloque(negocioId), "Precio", 0);

        mockMvc.perform(patch("/api/campos/{id}", campoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("""
                                {"nombre":"Precio","tipo":"NUMERO","obligatorio":true,"valorInicial":"0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("NUMERO"))
                .andExpect(jsonPath("$.obligatorio").value(true))
                .andExpect(jsonPath("$.valorInicial").value("0"));

        assertThat(campos.findByIdAndNegocioId(campoId, negocioId).orElseThrow().getTipo())
                .isEqualTo(TipoCampo.NUMERO);
    }

    @Test
    @DisplayName("Crear en un bloque ajeno devuelve 404 y no guarda nada")
    void crearShouldHideForeignBloque() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoBloqueId = givenBloque(ajenoNegocioId);

        crear(ajenoBloqueId, "{\"nombre\":\"Intruso\",\"tipo\":\"TEXTO\"}")
                .andExpect(status().isNotFound());

        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(ajenoBloqueId, ajenoNegocioId))
                .isEmpty();
    }

    @Test
    @DisplayName("Listar los campos de un bloque ajeno devuelve 404")
    void listarShouldHideForeignBloque() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoBloqueId = givenBloque(ajenoNegocioId);
        givenCampo(ajenoNegocioId, ajenoBloqueId, "Secreto", 0);

        mockMvc.perform(get("/api/bloques/{id}/campos", ajenoBloqueId).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un PATCH a un campo ajeno responde 404 y conserva su configuración")
    void editarShouldNotModifyForeignCampo() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoCampoId = givenCampo(ajenoNegocioId, givenBloque(ajenoNegocioId), "Ajeno", 0);

        mockMvc.perform(patch("/api/campos/{id}", ajenoCampoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"nombre\":\"Cambio\",\"tipo\":\"TEXTO\"}"))
                .andExpect(status().isNotFound());

        assertThat(campos.findByIdAndNegocioId(ajenoCampoId, ajenoNegocioId).orElseThrow().getNombre())
                .isEqualTo("Ajeno");
    }

    @Test
    @DisplayName("Un DELETE a un campo ajeno responde 404 y conserva el campo")
    void eliminarShouldNotDeleteForeignCampo() throws Exception {
        UUID ajenoNegocioId = givenNegocio(givenUsuario());
        UUID ajenoCampoId = givenCampo(ajenoNegocioId, givenBloque(ajenoNegocioId), "Ajeno", 0);

        mockMvc.perform(delete("/api/campos/{id}", ajenoCampoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNotFound());

        assertThat(campos.findByIdAndNegocioId(ajenoCampoId, ajenoNegocioId)).isPresent();
    }

    @Test
    @DisplayName("Un DELETE a un campo propio lo borra y responde 204")
    void eliminarShouldDeleteOwnCampo() throws Exception {
        UUID campoId = givenCampo(negocioId, givenBloque(negocioId), "Precio", 0);

        mockMvc.perform(delete("/api/campos/{id}", campoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());

        assertThat(campos.findByIdAndNegocioId(campoId, negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Crear un campo sin CSRF devuelve 403")
    void crearShouldRequireCsrf() throws Exception {
        UUID bloqueId = givenBloque(negocioId);

        mockMvc.perform(post("/api/bloques/{id}/campos", bloqueId).session(sesion)
                        .contentType("application/json").content("{\"nombre\":\"Producto\",\"tipo\":\"TEXTO\"}"))
                .andExpect(status().isForbidden());

        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Listar campos exige una sesión válida")
    void listarShouldRequireSession() throws Exception {
        mockMvc.perform(get("/api/bloques/{id}/campos", UUID.randomUUID()).session(givenSesionSinUsuario()))
                .andExpect(status().isUnauthorized());
    }

    // --- act ---
    private ResultActions crear(UUID bloqueId, String body) throws Exception {
        return mockMvc.perform(post("/api/bloques/{id}/campos", bloqueId).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(body));
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

    private UUID givenBloque(UUID negocioId) {
        UUID proyectoId = proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId)
                .nombre("Inventario")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre("Productos")
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenCampo(UUID negocioId, UUID bloqueId, String nombre, int posicion) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .nombre(nombre)
                .tipo(TipoCampo.TEXTO)
                .obligatorio(false)
                .opciones(List.of())
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
