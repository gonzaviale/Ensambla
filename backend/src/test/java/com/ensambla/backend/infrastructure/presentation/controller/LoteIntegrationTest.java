package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
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
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio de hecho de T-090 de punta a punta: PostgreSQL con Flyway, la carpeta privada de fotos que lee la IA y la
 * cadena de seguridad reales. Cada test hace rollback de la base; las fotos se escriben en una carpeta temporal.
 */
@Transactional
class LoteIntegrationTest extends PostgresRepositoryTestSupport {

    @TempDir
    static Path archivosRaiz;

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
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private ILoteSubidaJpaRepository lotes;

    @Autowired
    private IFotoCuadernoJpaRepository fotos;

    @Autowired
    private ArchivoCuadernoPort archivoCuadernoPort;

    private MockMvc mockMvc;
    private UUID negocioId;
    private MockHttpSession sesion;
    private String csrfToken;

    @DynamicPropertySource
    static void archivosDelCuaderno(DynamicPropertyRegistry registry) {
        registry.add("ensambla.ia.procesamiento.archivos-raiz", () -> archivosRaiz.toString());
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
    @DisplayName("La foto queda en la carpeta del negocio y la lectura de IA la encuentra con los mismos bytes")
    void subirShouldStoreFotoReadableByProcesamiento() throws Exception {
        UUID loteId = loteId(subir(sesion, foto(JPEG)));

        FotoCuadernoEntity foto = fotoDe(loteId);
        assertThat(foto.getClaveAlmacenamiento()).startsWith(negocioId + "/fotos/");
        assertThat(archivosRaiz.resolve(foto.getClaveAlmacenamiento())).exists();
        assertThat(archivoCuadernoPort.leer(negocioId, foto.getClaveAlmacenamiento())).isEqualTo(JPEG);
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
    @DisplayName("Otro negocio no puede consultar el lote: responde 404")
    void obtenerShouldHideLoteFromOtherNegocio() throws Exception {
        UUID loteId = loteId(subir(sesion, foto(JPEG)));
        MockHttpSession otraSesion = givenSesion(givenUsuarioConNegocio());

        ResultActions result = mockMvc.perform(get("/api/lotes/{id}", loteId).session(otraSesion));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El dueño consulta su lote y ve sus fotos")
    void obtenerShouldReturnOwnLote() throws Exception {
        UUID loteId = loteId(subir(sesion, foto(JPEG)));

        ResultActions result = mockMvc.perform(get("/api/lotes/{id}", loteId).session(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos[0].tipoContenido").value("image/jpeg"));
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
        UUID creadoId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(creadoId, AHORA.minusDays(1));
        return creadoId;
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
