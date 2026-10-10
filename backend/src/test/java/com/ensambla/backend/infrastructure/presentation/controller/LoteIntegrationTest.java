package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.enums.EstadoLote;
import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IFotoCuadernoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILoteSubidaJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.MinioTestContainer.MINIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio de hecho de T-090 de punta a punta: PostgreSQL con Flyway, MinIO y la cadena de seguridad reales.
 * Cada test hace rollback de la base; los archivos quedan en el MinIO del test, que se descarta al terminar.
 */
@Transactional
class LoteIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String BUCKET = "ensambla-test";

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

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
    private ILoteSubidaJpaRepository lotes;

    @Autowired
    private IFotoCuadernoJpaRepository fotos;

    private MockMvc mockMvc;
    private UUID negocioId;
    private MockHttpSession sesion;
    private String csrfToken;

    @DynamicPropertySource
    static void almacenamiento(DynamicPropertyRegistry registry) {
        registry.add("ensambla.almacenamiento.endpoint", MINIO::getS3URL);
        registry.add("ensambla.almacenamiento.access-key", MINIO::getUserName);
        registry.add("ensambla.almacenamiento.secret-key", MINIO::getPassword);
        registry.add("ensambla.almacenamiento.bucket", () -> BUCKET);
        registry.add("ensambla.almacenamiento.inicializar-bucket", () -> "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuario();
        negocioId = givenNegocio(usuarioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Una foto válida responde 201 con el id del lote pendiente guardado en el negocio de la sesión")
    void subirShouldCreatePendingLote() throws Exception {
        ResultActions result = subir(sesion, foto(JPEG));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        LoteSubidaEntity lote = lotes.findById(loteId(result)).orElseThrow();
        assertThat(lote.getNegocioId()).isEqualTo(negocioId);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("La foto queda en la carpeta del negocio dentro del bucket y el dueño la ve con la URL firmada")
    void subirShouldStoreFotoReadableByOwner() throws Exception {
        UUID loteId = loteId(subir(sesion, foto(JPEG)));

        String url = urlDeLectura(loteId);

        assertThat(fotoDe(loteId).getClaveAlmacenamiento()).startsWith("negocios/" + negocioId + "/fotos/");
        assertThat(leer(url).statusCode()).isEqualTo(200);
        assertThat(leer(url).body()).isEqualTo(JPEG);
    }

    @Test
    @DisplayName("Un archivo que no es una foto responde 400 con un mensaje claro y no crea el lote")
    void subirShouldRejectInvalidFile() throws Exception {
        long lotesAntes = lotes.count();

        ResultActions result = subir(sesion, foto("no soy una foto".getBytes(StandardCharsets.UTF_8)));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
        assertThat(lotes.count()).isEqualTo(lotesAntes);
    }

    @Test
    @DisplayName("Otro negocio no puede consultar el lote: responde 404 y no recibe URLs")
    void obtenerShouldHideLoteFromOtherNegocio() throws Exception {
        UUID loteId = loteId(subir(sesion, foto(JPEG)));
        MockHttpSession otraSesion = givenSesion(givenUsuarioConNegocio());

        ResultActions result = mockMvc.perform(get("/api/lotes/{id}", loteId).session(otraSesion));

        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fotos").doesNotExist());
    }

    @Test
    @DisplayName("Sin firma, la foto no se puede leer directo del bucket ni conociendo su clave")
    void fotoShouldNotBeReadableWithoutSignature() throws Exception {
        UUID loteId = loteId(subir(sesion, foto(JPEG)));
        String urlSinFirma = MINIO.getS3URL() + "/" + BUCKET + "/" + fotoDe(loteId).getClaveAlmacenamiento();

        HttpResponse<byte[]> respuesta = leer(urlSinFirma);

        assertThat(respuesta.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Subir sin el token CSRF responde 403")
    void subirShouldRequireCsrf() throws Exception {
        ResultActions result = mockMvc.perform(multipart("/api/lotes").file(foto(JPEG)).session(sesion));

        result.andExpect(status().isForbidden());
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

    private UUID givenUsuarioConNegocio() {
        UUID usuarioId = givenUsuario();
        givenNegocio(usuarioId);
        return usuarioId;
    }

    private MockHttpSession givenSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = json.readValue(body, CsrfResponse.class).token();
        return session;
    }

    // --- act ---
    private ResultActions subir(MockHttpSession session, MockMultipartFile archivo) throws Exception {
        return mockMvc.perform(multipart("/api/lotes")
                .file(archivo)
                .session(session)
                .header("X-CSRF-TOKEN", csrfToken));
    }

    private String urlDeLectura(UUID loteId) throws Exception {
        String body = mockMvc.perform(get("/api/lotes/{id}", loteId).session(sesion))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("fotos").path(0).path("url").asString();
    }

    private HttpResponse<byte[]> leer(String url) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    // --- helpers ---
    private MockMultipartFile foto(byte[] contenido) {
        return new MockMultipartFile("fotos", "pagina.jpg", "image/jpeg", contenido);
    }

    private UUID loteId(ResultActions result) throws Exception {
        JsonNode body = json.readTree(result.andReturn().getResponse().getContentAsString());
        return UUID.fromString(body.path("id").asString());
    }

    private FotoCuadernoEntity fotoDe(UUID loteId) {
        return fotos.findAllByLoteIdAndNegocioIdOrderByPosicionAsc(loteId, negocioId).getFirst();
    }
}
