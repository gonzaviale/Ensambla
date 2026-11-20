package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuario;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuarioRequest;
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
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
    private RegistrarUsuario registrarUsuario;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Csrf csrf;
    private Cookie cookie;
    private ResultActions respuesta;
    private MvcResult resultado;
    private Throwable fallo;

    @BeforeEach
    @AfterEach
    void limpiarLasCuentas() {
        negocioJpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM spring_session");
    }

    @Test
    @DisplayName("Registro válido responde 201 y persiste un negocio y un hash sin abrir sesión")
    void registroValidoResponde201YPersisteUnNegocioYUnHashSinAbrirSesion() throws Exception {
        dadoUnTokenCsrfSinSesion();

        cuandoSeRegistraUnaCuenta(REGISTRO);

        entoncesSeRegistraConElNegocioYElHash();
        entoncesNoSeAbreSesion();
    }

    @Test
    @DisplayName("Email repetido con distinta capitalización responde 409 sin crear otro negocio")
    void emailRepetidoConDistintaCapitalizacionResponde409SinCrearOtroNegocio() throws Exception {
        dadoUnaCuentaRegistrada();

        cuandoSeRegistraUnaCuenta(REGISTRO);

        entoncesResponde409PorElEmailRepetido();
        entoncesExisteUnaSolaCuentaConSuNegocio();
    }

    @Test
    @DisplayName("Si falla la creación del negocio revierte el usuario en PostgreSQL")
    void siFallaLaCreacionDelNegocioRevierteElUsuarioEnPostgresql() {
        cuandoSeRegistraUnaCuentaSinNombreDeNegocio();

        entoncesSeRevierteElUsuario();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"nombre\":\"Brian\",\"nombreNegocio\":\"Negocio\",\"email\":\"invalido\",\"contrasenia\":\"Clave1234!\"}",
            "{\"nombre\":\"Brian\",\"nombreNegocio\":\" \",\"email\":\"a@b.com\",\"contrasenia\":\"Clave1234!\"}",
            "{\"nombre\":\"Brian\",\"nombreNegocio\":\"Negocio\",\"email\":\"a@b.com\",\"contrasenia\":\"corta\"}"
    })
    @DisplayName("Registro inválido responde 400 sin persistir datos")
    void registroInvalidoResponde400SinPersistirDatos(String cuerpo) throws Exception {
        dadoUnTokenCsrfSinSesion();

        cuandoSeRegistraUnaCuenta(cuerpo);

        entoncesSeRechazaElRegistroSinPersistirNada();
    }

    @Test
    @DisplayName("Contraseña Unicode de más de 72 bytes devuelve validación de campo")
    void contraseniaUnicodeDeMasDe72BytesDevuelveValidacionDeCampo() throws Exception {
        dadoUnTokenCsrfSinSesion();

        cuandoSeRegistraUnaCuenta(REGISTRO.replace(CONTRASENIA, "ñ".repeat(37)));

        entoncesSeRechazaLaContraseniaSinPersistirNada();
    }

    @Test
    @DisplayName("Login correcto rota la cookie, persiste la sesión y expone solo identidad pública")
    void loginCorrectoRotaLaCookiePersisteLaSesionYExponeSoloIdentidadPublica() throws Exception {
        dadoUnaCuentaRegistrada();

        cuandoSeIniciaSesion(LOGIN);

        entoncesSeRotaLaCookieDeSesion();
        entoncesLaSesionExponeSoloLaIdentidadPublica();
        entoncesLaSesionPersistidaDuraDosHoras();
    }

    @Test
    @DisplayName("Contraseña incorrecta devuelve 401 sin abrir sesión")
    void contraseniaIncorrectaDevuelve401SinAbrirSesion() throws Exception {
        dadoUnaCuentaRegistrada();

        cuandoSeIniciaSesion(LOGIN.replace(CONTRASENIA, "incorrecta"));

        entoncesSeRechazaElLoginSinAbrirSesion();
    }

    @Test
    @DisplayName("Email desconocido devuelve 401")
    void emailDesconocidoDevuelve401() throws Exception {
        dadoUnTokenCsrfSinSesion();

        cuandoSeIniciaSesion(LOGIN);

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Un usuario suspendido no puede iniciar sesión")
    void unUsuarioSuspendidoNoPuedeIniciarSesion() throws Exception {
        dadoUnaCuentaRegistrada();
        dadoUnUsuarioSuspendido();

        cuandoSeIniciaSesion(LOGIN);

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Logout elimina la sesión JDBC y una cookie anterior ya no autentica")
    void logoutEliminaLaSesionJdbcYUnaCookieAnteriorYaNoAutentica() throws Exception {
        dadoUnaSesionIniciada();

        cuandoSeCierraLaSesion();

        entoncesSeEliminaLaSesionPersistida();
        entoncesLaCookieAnteriorYaNoAutentica();
    }

    @Test
    @DisplayName("Una sesión vencida responde 401")
    void unaSesionVencidaResponde401() throws Exception {
        dadoUnaSesionIniciada();
        dadoQueLaSesionVencio();

        cuandoSeConsultaLaSesionActual();

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Suspender la cuenta invalida la sesión que ya estaba abierta")
    void suspenderLaCuentaInvalidaLaSesionQueYaEstabaAbierta() throws Exception {
        dadoUnaSesionIniciada();
        dadoUnUsuarioSuspendido();

        cuandoSeConsultaLaSesionActual();

        entoncesNoAutoriza();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/proyectos", "/api/bloques", "/api/campos", "/api/registros"})
    @DisplayName("Todo endpoint privado sin sesión responde 401")
    void todoEndpointPrivadoSinSesionResponde401(String ruta) throws Exception {
        cuandoSeConsultaSinSesion(ruta);

        entoncesNoAutorizaConEstado401();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("También las escrituras privadas sin sesión responden 401 antes de CSRF")
    void tambienLasEscriturasPrivadasSinSesionResponden401AntesDeCsrf(String metodo) throws Exception {
        cuandoSeEscribeSinSesion(metodo);

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Una escritura con sesión pero sin CSRF devuelve 403")
    void unaEscrituraConSesionPeroSinCsrfDevuelve403() throws Exception {
        dadoUnaSesionIniciada();

        cuandoSeEscribeConSesionSinCsrf();

        entoncesSeProhibe();
    }

    @Test
    @DisplayName("Logout sin sesión es idempotente si el CSRF es válido")
    void logoutSinSesionEsIdempotenteSiElCsrfEsValido() throws Exception {
        dadoUnTokenCsrfSinSesion();

        cuandoSeCierraLaSesionSinHaberlaAbierto();

        entoncesSeCierraSinContenido();
    }

    // --- dado ---

    private void dadoUnTokenCsrfSinSesion() throws Exception {
        csrf = pedirTokenCsrf(null);
    }

    private void dadoUnaCuentaRegistrada() throws Exception {
        dadoUnTokenCsrfSinSesion();
        cuandoSeRegistraUnaCuenta(REGISTRO);
        respuesta.andExpect(status().isCreated());
    }

    private void dadoUnaSesionIniciada() throws Exception {
        dadoUnaCuentaRegistrada();
        cuandoSeIniciaSesion(LOGIN);
        cookie = resultado.getResponse().getCookie("ENSAMBLA_SESSION");
    }

    private void dadoUnUsuarioSuspendido() {
        UsuarioEntity entidad = usuarioJpaRepository.findByEmail(EMAIL).orElseThrow();
        entidad.setEstado(EstadoUsuario.SUSPENDIDO);
        usuarioJpaRepository.saveAndFlush(entidad);
    }

    private void dadoQueLaSesionVencio() {
        jdbcTemplate.update("UPDATE spring_session SET last_access_time = 0, expiry_time = 0 WHERE session_id = ?",
                idDeSesion(cookie));
    }

    // --- cuando ---

    private void cuandoSeRegistraUnaCuenta(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post("/api/auth/register").cookie(csrf.cookie())
                .header("X-CSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void cuandoSeRegistraUnaCuentaSinNombreDeNegocio() {
        RegistrarUsuarioRequest invalido = new RegistrarUsuarioRequest("Brian", "", EMAIL, CONTRASENIA);
        fallo = catchThrowable(() -> registrarUsuario.execute(invalido));
    }

    private void cuandoSeIniciaSesion(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                .header("X-CSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        resultado = respuesta.andReturn();
    }

    private void cuandoSeCierraLaSesion() throws Exception {
        Csrf csrfConSesion = pedirTokenCsrf(cookie);
        respuesta = mockMvc.perform(post("/api/auth/logout")
                .cookie(cookie).header("X-CSRF-TOKEN", csrfConSesion.token()));
    }

    private void cuandoSeCierraLaSesionSinHaberlaAbierto() throws Exception {
        respuesta = mockMvc.perform(post("/api/auth/logout")
                .cookie(csrf.cookie()).header("X-CSRF-TOKEN", csrf.token()));
    }

    private void cuandoSeConsultaLaSesionActual() throws Exception {
        respuesta = mockMvc.perform(get("/api/auth/me").cookie(cookie));
    }

    private void cuandoSeConsultaSinSesion(String ruta) throws Exception {
        respuesta = mockMvc.perform(get(ruta));
    }

    private void cuandoSeEscribeSinSesion(String metodo) throws Exception {
        respuesta = mockMvc.perform(request(HttpMethod.valueOf(metodo), "/api/proyectos"));
    }

    private void cuandoSeEscribeConSesionSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos").cookie(cookie));
    }

    // --- entonces ---

    private void entoncesSeRegistraConElNegocioYElHash() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.contrasenia").doesNotExist())
                .andExpect(jsonPath("$.hashContrasenia").doesNotExist());
        UsuarioEntity entidad = usuarioJpaRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches(CONTRASENIA, entidad.getHashContrasenia())).isTrue();
        assertThat(entidad.getHashContrasenia()).startsWith("{bcrypt}").doesNotContain(CONTRASENIA);
        assertThat(entidad.getNombre()).isEqualTo("Brian");
        assertThat(entidad.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(entidad.getCreadoEn()).isNotNull();
        assertThat(entidad.getActualizadoEn()).isEqualTo(entidad.getCreadoEn());
        var negocios = negocioJpaRepository.findAll();
        assertThat(negocios).hasSize(1);
        assertThat(negocios.getFirst().getNombre()).isEqualTo("Mi negocio");
        assertThat(negocios.getFirst().getDuenioId()).isEqualTo(entidad.getId());
    }

    private void entoncesNoSeAbreSesion() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(csrf.cookie())).andExpect(status().isUnauthorized());
    }

    private void entoncesResponde409PorElEmailRepetido() throws Exception {
        respuesta.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese email"));
    }

    private void entoncesExisteUnaSolaCuentaConSuNegocio() {
        assertThat(usuarioJpaRepository.count()).isEqualTo(1);
        assertThat(negocioJpaRepository.count()).isEqualTo(1);
    }

    private void entoncesSeRevierteElUsuario() {
        assertThat(fallo).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(usuarioJpaRepository.findByEmail(EMAIL)).isEmpty();
        assertThat(negocioJpaRepository.count()).isZero();
    }

    private void entoncesSeRechazaElRegistroSinPersistirNada() throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        assertThat(usuarioJpaRepository.count()).isZero();
        assertThat(negocioJpaRepository.count()).isZero();
    }

    private void entoncesSeRechazaLaContraseniaSinPersistirNada() throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.contrasenia").exists());
        assertThat(usuarioJpaRepository.count()).isZero();
    }

    private void entoncesSeRotaLaCookieDeSesion() throws Exception {
        respuesta.andExpect(status().isOk());
        Cookie nueva = resultado.getResponse().getCookie("ENSAMBLA_SESSION");
        assertThat(nueva).isNotNull();
        assertThat(nueva.getValue()).isNotEqualTo(csrf.cookie().getValue());
        assertThat(resultado.getResponse().getHeaders("Set-Cookie").toString())
                .contains("HttpOnly").contains("SameSite=Lax");
    }

    private void entoncesLaSesionExponeSoloLaIdentidadPublica() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(resultado.getResponse().getCookie("ENSAMBLA_SESSION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Brian"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.hashContrasenia").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    private void entoncesLaSesionPersistidaDuraDosHoras() {
        Long duracion = jdbcTemplate.queryForObject(
                "SELECT max_inactive_interval FROM spring_session WHERE session_id = ?", Long.class,
                idDeSesion(resultado.getResponse().getCookie("ENSAMBLA_SESSION")));
        assertThat(duracion).isEqualTo(7200);
    }

    private void entoncesSeRechazaElLoginSinAbrirSesion() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").cookie(csrf.cookie())).andExpect(status().isUnauthorized());
    }

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesNoAutorizaConEstado401() throws Exception {
        respuesta.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    private void entoncesSeEliminaLaSesionPersistida() throws Exception {
        respuesta.andExpect(status().isNoContent()).andExpect(header().string("Cache-Control", "no-store"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM spring_session WHERE session_id = ?", Long.class, idDeSesion(cookie)))
                .isZero();
    }

    private void entoncesLaCookieAnteriorYaNoAutentica() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/proyectos").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    private void entoncesSeCierraSinContenido() throws Exception {
        respuesta.andExpect(status().isNoContent());
    }

    // --- datos ---

    private Csrf pedirTokenCsrf(Cookie cookieExistente) throws Exception {
        var pedido = get("/api/auth/csrf");
        if (cookieExistente != null) {
            pedido.cookie(cookieExistente);
        }
        MvcResult respuestaCsrf = mockMvc.perform(pedido).andExpect(status().isOk()).andReturn();
        Cookie cookieNueva = respuestaCsrf.getResponse().getCookie("ENSAMBLA_SESSION");
        String token = JsonPath.read(respuestaCsrf.getResponse().getContentAsString(), "$.token");
        return new Csrf(cookieNueva == null ? cookieExistente : cookieNueva, token);
    }

    private String idDeSesion(Cookie cookieDeSesion) {
        return new String(Base64.getDecoder().decode(cookieDeSesion.getValue()), StandardCharsets.UTF_8);
    }

    private record Csrf(Cookie cookie, String token) {
    }
}
