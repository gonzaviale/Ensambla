package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IFotoCuadernoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILoteSubidaJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockMultipartFile;
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
    private NegocioId negocioId;
    private SesionHttpDePrueba sesion;
    private ResultActions respuesta;
    private LoteId loteId;
    private long lotesAntes;

    @DynamicPropertySource
    static void archivosDelCuaderno(DynamicPropertyRegistry registry) {
        registry.add("ensambla.ia.procesamiento.archivos-raiz", () -> archivosRaiz.toString());
    }

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UsuarioId usuarioId = UsuarioId.de(crearUsuario());
        negocioId = NegocioId.de(crearNegocio(usuarioId));
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @Test
    @DisplayName("Una foto válida responde 201 con el id del lote pendiente guardado en el negocio de la sesión")
    void unaFotoValidaResponde201ConElIdDelLotePendienteGuardadoEnElNegocioDeLaSesion() throws Exception {
        cuandoSeSubeUnaFoto(JPEG);

        entoncesSeCreaUnLotePendienteEnElNegocioDeLaSesion();
    }

    @Test
    @DisplayName("La foto queda en la carpeta del negocio y la lectura de IA la encuentra con los mismos bytes")
    void laFotoQuedaEnLaCarpetaDelNegocioYLaLecturaDeIaLaEncuentraConLosMismosBytes() throws Exception {
        cuandoSeSubeUnaFoto(JPEG);

        entoncesLaFotoQuedaEnLaCarpetaDelNegocioYSeLeeIgual();
    }

    @Test
    @DisplayName("Un archivo que no es una foto responde 400 con un mensaje claro y no crea el lote")
    void unArchivoQueNoEsUnaFotoResponde400ConUnMensajeClaroYNoCreaElLote() throws Exception {
        dadoLaCantidadDeLotesActual();

        cuandoSeSubeUnaFoto("no soy una foto".getBytes(StandardCharsets.UTF_8));

        entoncesSeRechazaElArchivoSinCrearElLote();
    }

    @Test
    @DisplayName("Otro negocio no puede consultar el lote: responde 404")
    void otroNegocioNoPuedeConsultarElLoteRespondeNoEncontrado() throws Exception {
        dadoUnLoteSubido();
        dadoUnaSesionDeOtroNegocio();

        cuandoSeConsultaElLote();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("El dueño consulta su lote y ve sus fotos")
    void elDuenioConsultaSuLoteYVeSusFotos() throws Exception {
        dadoUnLoteSubido();

        cuandoSeConsultaElLote();

        entoncesSeVeLaFotoDelLote();
    }

    @Test
    @DisplayName("Subir sin el token CSRF responde 403")
    void subirSinElTokenCsrfResponde403() throws Exception {
        cuandoSeSubeUnaFotoSinCsrf();

        entoncesSeProhibe();
    }

    // --- dado ---

    private void dadoLaCantidadDeLotesActual() {
        lotesAntes = lotes.count();
    }

    private void dadoUnLoteSubido() throws Exception {
        cuandoSeSubeUnaFoto(JPEG);
        loteId = idDelLoteCreado();
    }

    private void dadoUnaSesionDeOtroNegocio() throws Exception {
        UsuarioId otroUsuario = UsuarioId.de(crearUsuario());
        crearNegocio(otroUsuario);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, otroUsuario.toString());
    }

    // --- cuando ---

    private void cuandoSeSubeUnaFoto(byte[] contenido) throws Exception {
        respuesta = mockMvc.perform(multipart("/api/lotes")
                .file(foto(contenido))
                .session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    private void cuandoSeSubeUnaFotoSinCsrf() throws Exception {
        respuesta = mockMvc.perform(multipart("/api/lotes").file(foto(JPEG)).session(sesion.sesion()));
    }

    private void cuandoSeConsultaElLote() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}", loteId).session(sesion.sesion()));
    }

    // --- entonces ---

    private void entoncesSeCreaUnLotePendienteEnElNegocioDeLaSesion() throws Exception {
        respuesta.andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        LoteSubidaEntity lote = lotes.findById(idDelLoteCreado().valor()).orElseThrow();
        assertThat(lote.getNegocioId()).isEqualTo(negocioId.valor());
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
    }

    private void entoncesLaFotoQuedaEnLaCarpetaDelNegocioYSeLeeIgual() throws Exception {
        FotoCuadernoEntity foto = fotoDe(idDelLoteCreado());
        assertThat(foto.getClaveAlmacenamiento()).startsWith(negocioId + "/fotos/");
        assertThat(archivosRaiz.resolve(foto.getClaveAlmacenamiento())).exists();
        assertThat(archivoCuadernoPort.leer(negocioId, foto.getClaveAlmacenamiento())).isEqualTo(JPEG);
    }

    private void entoncesSeRechazaElArchivoSinCrearElLote() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
        assertThat(lotes.count()).isEqualTo(lotesAntes);
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesSeVeLaFotoDelLote() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos[0].tipoContenido").value("image/jpeg"));
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    // --- datos ---

    private UUID crearUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearNegocio(UsuarioId usuarioId) {
        UUID creadoId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creadoId), AHORA.minusDays(1));
        return creadoId;
    }

    private MockMultipartFile foto(byte[] contenido) {
        return new MockMultipartFile("fotos", "pagina.jpg", "image/jpeg", contenido);
    }

    private LoteId idDelLoteCreado() throws Exception {
        JsonNode cuerpo = json.readTree(respuesta.andReturn().getResponse().getContentAsString());
        return LoteId.de(UUID.fromString(cuerpo.path("id").asString()));
    }

    private FotoCuadernoEntity fotoDe(LoteId lote) {
        return fotos.findAllByLoteIdAndNegocioIdOrderByPosicionAsc(lote.valor(), negocioId.valor()).getFirst();
    }
}
