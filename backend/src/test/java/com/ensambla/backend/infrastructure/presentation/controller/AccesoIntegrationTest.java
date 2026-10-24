package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccesoIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String EMAIL = "sesion@example.com";
    private static final String CONTRASENIA = "ClaveSegura123!";
    private static final String REGISTRO = """
            {"nombre":" Brian ","nombreNegocio":" Mi negocio ","email":"SESION@EXAMPLE.COM",
             "contrasenia":"ClaveSegura123!"}
            """;
    private static final String LOGIN = """
            {"email":"sesion@example.com","contrasenia":"ClaveSegura123!"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IUsuarioJpaRepository usuarioJpaRepository;
    @Autowired
    private INegocioJpaRepository negocioJpaRepository;
    @Autowired
    private RegistroService registroService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanAccounts() {
        negocioJpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM spring_session");
    }

    @Test
    @DisplayName("Registro válido responde 201 y persiste un negocio y un hash sin abrir sesión")
    void registrarShouldCreateUsuarioAndNegocioAtomically() throws Exception {
        Csrf csrf = givenCsrf(null);

        ResultActions result = registrar(csrf, REGISTRO);

        thenRegisteredWithHash(result);
        mockMvc.perform(get("/api/auth/me").cookie(csrf.cookie())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Email repetido con distinta capitalización responde 409 sin crear otro negocio")
    void registrarShouldRejectDuplicateEmail() throws Exception {
        Csrf csrf = givenCsrf(null);
        registrar(csrf, REGISTRO).andExpect(status().isCreated());

        ResultActions result = registrar(csrf, REGISTRO);

        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese email"));
        assertThat(usuarioJpaRepository.count()).isEqualTo(1);
        assertThat(negocioJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si falla la creación del negocio revierte el usuario en PostgreSQL")
    void registrarShouldRollbackUsuarioWhenNegocioFails() {
        RegistrarUsuarioRequest invalid = new RegistrarUsuarioRequest("Brian", "", EMAIL, CONTRASENIA);

        assertThatThrownBy(() -> registroService.registrar(invalid, OffsetDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(usuarioJpaRepository.findByEmail(EMAIL)).isEmpty();
        assertThat(negocioJpaRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"nombre\":\"Brian\",\"nombreNegocio\":\"Negocio\",\"email\":\"invalido\",\"contrasenia\":\"Clave1234!\"}",
            "{\"nombre\":\"Brian\",\"nombreNegocio\":\" \",\"email\":\"a@b.com\",\"contrasenia\":\"Clave1234!\"}",
            "{\"nombre\":\"Brian\",\"nombreNegocio\":\"Negocio\",\"email\":\"a@b.com\",\"contrasenia\":\"corta\"}"
    })
    @DisplayName("Registro inválido responde 400 sin persistir datos")
    void registrarShouldRejectInvalidBody(String body) throws Exception {
        Csrf csrf = givenCsrf(null);

        ResultActions result = registrar(csrf, body);

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        assertThat(usuarioJpaRepository.count()).isZero();
        assertThat(negocioJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Contraseña Unicode de más de 72 bytes devuelve validación de campo")
    void registrarShouldRejectOversizedUnicodePassword() throws Exception {
        Csrf csrf = givenCsrf(null);
        String body = REGISTRO.replace(CONTRASENIA, "ñ".repeat(37));

        ResultActions result = registrar(csrf, body);

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.contrasenia").exists());
        assertThat(usuarioJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Login correcto rota la cookie, persiste la sesión y expone solo identidad pública")
    void loginShouldRotateCookieAndPersistIdentity() throws Exception {
        Csrf csrf = givenRegisteredAccount();

        MvcResult result = login(csrf, LOGIN).andExpect(status().isOk()).andReturn();

        Cookie cookie = result.getResponse().getCookie("ENSAMBLA_SESSION");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotEqualTo(csrf.cookie().getValue());
        assertThat(result.getResponse().getHeaders("Set-Cookie").toString())
                .contains("HttpOnly").contains("SameSite=Lax");
        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Brian"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.hashContrasenia").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
        Long timeout = jdbcTemplate.queryForObject(
                "SELECT max_inactive_interval FROM spring_session WHERE session_id = ?", Long.class,
                decodeSessionId(cookie));
        assertThat(timeout).isEqualTo(7200);
    }

    @Test
    @DisplayName("Contraseña incorrecta devuelve 401 sin abrir sesión")
    void loginShouldRejectWrongPassword() throws Exception {
        Csrf csrf = givenRegisteredAccount();

        ResultActions result = login(csrf, LOGIN.replace(CONTRASENIA, "incorrecta"));

        result.andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").cookie(csrf.cookie())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Email desconocido devuelve 401")
    void loginShouldRejectUnknownEmail() throws Exception {
        Csrf csrf = givenCsrf(null);

        ResultActions result = login(csrf, LOGIN);

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un usuario suspendido no puede iniciar sesión")
    void loginShouldRejectSuspendedUsuario() throws Exception {
        Csrf csrf = givenRegisteredAccount();
        givenSuspendedUsuario();

        ResultActions result = login(csrf, LOGIN);

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout elimina la sesión JDBC y una cookie anterior ya no autentica")
    void logoutShouldInvalidatePreviousCookie() throws Exception {
        Cookie cookie = givenLogin();
        Csrf csrf = givenCsrf(cookie);

        ResultActions result = mockMvc.perform(post("/api/auth/logout")
                .cookie(cookie).header("X-CSRF-TOKEN", csrf.token()));

        result.andExpect(status().isNoContent()).andExpect(header().string("Cache-Control", "no-store"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM spring_session WHERE session_id = ?", Long.class, decodeSessionId(cookie)))
                .isZero();
        mockMvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/proyectos").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una sesión vencida responde 401")
    void meShouldRejectExpiredSession() throws Exception {
        Cookie cookie = givenLogin();
        jdbcTemplate.update("UPDATE spring_session SET last_access_time = 0, expiry_time = 0 WHERE session_id = ?",
                decodeSessionId(cookie));

        ResultActions result = mockMvc.perform(get("/api/auth/me").cookie(cookie));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Suspender la cuenta invalida la sesión que ya estaba abierta")
    void meShouldInvalidateSuspendedUsuarioSession() throws Exception {
        Cookie cookie = givenLogin();
        givenSuspendedUsuario();

        ResultActions result = mockMvc.perform(get("/api/auth/me").cookie(cookie));

        result.andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/proyectos", "/api/bloques", "/api/campos", "/api/registros"})
    @DisplayName("Todo endpoint privado sin sesión responde 401")
    void privateEndpointShouldReturn401WithoutSession(String path) throws Exception {
        ResultActions result = mockMvc.perform(get(path));

        result.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("También las escrituras privadas sin sesión responden 401 antes de CSRF")
    void privateWriteShouldReturn401WithoutSession(String method) throws Exception {
        ResultActions result = mockMvc.perform(request(HttpMethod.valueOf(method), "/api/proyectos"));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una escritura con sesión pero sin CSRF devuelve 403")
    void privateWriteShouldRequireCsrfWhenAuthenticated() throws Exception {
        Cookie cookie = givenLogin();

        ResultActions result = mockMvc.perform(post("/api/proyectos").cookie(cookie));

        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Logout sin sesión es idempotente si el CSRF es válido")
    void logoutShouldBeIdempotent() throws Exception {
        Csrf csrf = givenCsrf(null);

        ResultActions result = mockMvc.perform(post("/api/auth/logout")
                .cookie(csrf.cookie()).header("X-CSRF-TOKEN", csrf.token()));

        result.andExpect(status().isNoContent());
    }

    // --- arrange ---
    private Csrf givenCsrf(Cookie cookie) throws Exception {
        var builder = get("/api/auth/csrf");
        if (cookie != null) {
            builder.cookie(cookie);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        Cookie responseCookie = result.getResponse().getCookie("ENSAMBLA_SESSION");
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        return new Csrf(responseCookie == null ? cookie : responseCookie, token);
    }

    private Csrf givenRegisteredAccount() throws Exception {
        Csrf csrf = givenCsrf(null);
        registrar(csrf, REGISTRO).andExpect(status().isCreated());
        return csrf;
    }

    private Cookie givenLogin() throws Exception {
        return login(givenRegisteredAccount(), LOGIN).andExpect(status().isOk())
                .andReturn().getResponse().getCookie("ENSAMBLA_SESSION");
    }

    private void givenSuspendedUsuario() {
        UsuarioEntity entity = usuarioJpaRepository.findByEmail(EMAIL).orElseThrow();
        entity.setEstado(EstadoUsuario.SUSPENDIDO);
        usuarioJpaRepository.saveAndFlush(entity);
    }

    // --- act ---
    private ResultActions registrar(Csrf csrf, String body) throws Exception {
        return mockMvc.perform(post("/api/auth/register").cookie(csrf.cookie())
                .header("X-CSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(Csrf csrf, String body) throws Exception {
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                .header("X-CSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // --- assert ---
    private void thenRegisteredWithHash(ResultActions result) throws Exception {
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.contrasenia").doesNotExist())
                .andExpect(jsonPath("$.hashContrasenia").doesNotExist());
        UsuarioEntity entity = usuarioJpaRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches(CONTRASENIA, entity.getHashContrasenia())).isTrue();
        assertThat(entity.getHashContrasenia()).startsWith("{bcrypt}").doesNotContain(CONTRASENIA);
        assertThat(entity.getNombre()).isEqualTo("Brian");
        assertThat(entity.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(entity.getCreadoEn()).isNotNull();
        assertThat(entity.getActualizadoEn()).isEqualTo(entity.getCreadoEn());
        var negocios = negocioJpaRepository.findAll();
        assertThat(negocios).hasSize(1);
        assertThat(negocios.getFirst().getNombre()).isEqualTo("Mi negocio");
        assertThat(negocios.getFirst().getDuenioId()).isEqualTo(entity.getId());
    }

    private String decodeSessionId(Cookie cookie) {
        return new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
    }

    private record Csrf(Cookie cookie, String token) {
    }
}

