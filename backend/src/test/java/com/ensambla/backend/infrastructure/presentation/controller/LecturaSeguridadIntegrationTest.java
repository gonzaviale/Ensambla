package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.ProcesamientoTestData;
import com.ensambla.backend.support.SesionHttpDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LecturaSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private ProcesamientoRepository procesamientoRepository;

    private MockMvc mockMvc;
    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private LoteId loteId;
    private SesionHttpDePrueba sesion;
    private UUID otroUsuario;
    private UUID otroNegocio;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        loteId = LoteId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Negocio', ?)",
                negocioId.valor(), usuarioId.valor());
        suscripcionService.asignarGratuito(negocioId, AHORA.minusDays(1));
        jdbcTemplate.update("INSERT INTO lote_subida (id, negocio_id) VALUES (?, ?)", loteId.valor(),
                negocioId.valor());
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @AfterEach
    void limpiarLasCuentas() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId.valor());
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId.valor());
        borrarLaOtraCuenta();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/lotes/20000000-0000-0000-0000-000000000001/lectura", "/api/ia/consumo"})
    @DisplayName("Las consultas de IA exigen sesión")
    void lasConsultasDeIaExigenSesion(String ruta) throws Exception {
        cuandoSeConsultaSinSesion(ruta);

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Reintentar sin sesión devuelve 401 antes de validar CSRF")
    void reintentarSinSesionDevuelve401AntesDeValidarCsrf() throws Exception {
        cuandoSeReintentaSinSesion();

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Reintentar con sesión pero sin CSRF devuelve 403")
    void reintentarConSesionPeroSinCsrfDevuelve403() throws Exception {
        cuandoSeReintentaSinCsrf();

        entoncesSeProhibe();
    }

    @Test
    @DisplayName("Consultar estado pendiente no llama al proveedor ni procesa el lote")
    void consultarEstadoPendienteNoLlamaAlProveedorNiProcesaElLote() throws Exception {
        cuandoSeConsultaLaLectura();

        entoncesLaLecturaEstaPendienteYNoSeProceso();
    }

    @Test
    @DisplayName("Consultar un lote desde otro negocio devuelve 404")
    void consultarUnLoteDesdeOtroNegocioDevuelve404() throws Exception {
        dadoUnaSesionDeOtroNegocio();

        cuandoSeConsultaLaLectura();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("La respuesta lista incluye estructura, mapeo, filas, confianza y consumo")
    void laRespuestaListaIncluyeEstructuraMapeoFilasConfianzaYConsumo() throws Exception {
        dadoUnaLecturaTerminadaConPropuesta();

        cuandoSeConsultaLaLectura();

        entoncesSeDevuelveLaPropuestaCompletaYTipada();
    }

    @Test
    @DisplayName("Reintentar ERROR devuelve 202 y vuelve a PENDIENTE")
    void reintentarErrorDevuelve202YVuelveAPendiente() throws Exception {
        dadoUnaLecturaTerminadaConError();

        cuandoSeReintenta();

        entoncesSeAceptaYElLoteVuelveAPendiente();
    }

    @Test
    @DisplayName("Reintentar un lote pendiente devuelve un mensaje claro")
    void reintentarUnLotePendienteDevuelveUnMensajeClaro() throws Exception {
        cuandoSeReintenta();

        entoncesSeRechazaPorqueSoloSeReintentaUnLoteEnError();
    }

    @Test
    @DisplayName("El consumo corresponde al negocio de la sesión aunque se envíe otro en la consulta")
    void elConsumoCorrespondeAlNegocioDeLaSesionAunqueSeEnvieOtroEnLaConsulta() throws Exception {
        dadoUnConsumoDeCincoPaginasYDosReservadas();

        cuandoSeConsultaElConsumoConOtroNegocio();

        entoncesSeDevuelveElConsumoDelNegocioDeLaSesion();
    }

    @Test
    @DisplayName("Un identificador inválido devuelve 400")
    void unIdentificadorInvalidoDevuelve400() throws Exception {
        cuandoSeConsultaUnaLecturaConIdInvalido();

        entoncesSeRechazaElPedido();
    }

    // --- dado ---

    private void dadoUnaSesionDeOtroNegocio() {
        otroUsuario = UUID.randomUUID();
        otroNegocio = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Otra persona')",
                otroUsuario, otroUsuario + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Otro', ?)",
                otroNegocio, otroUsuario);
        sesion.iniciarComo(otroUsuario.toString());
    }

    private void dadoUnaLecturaTerminadaConPropuesta() {
        var lote = procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2)).orElseThrow();
        procesamientoRepository.reservar(lote, 1, 100, AHORA);
        procesamientoRepository.finalizar(lote, new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false),
                BigDecimal.ZERO, AHORA, null);
    }

    private void dadoUnaLecturaTerminadaConError() {
        var lote = procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2)).orElseThrow();
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Falló", false), null, AHORA, null);
    }

    private void dadoUnConsumoDeCincoPaginasYDosReservadas() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas, reservadas) VALUES (?, ?, 5, 2)
                """,
                negocioId.valor(), ZonaNegocio.dia(ahora));
    }

    // --- cuando ---

    private void cuandoSeConsultaSinSesion(String ruta) throws Exception {
        respuesta = mockMvc.perform(get(ruta));
    }

    private void cuandoSeReintentaSinSesion() throws Exception {
        respuesta = mockMvc.perform(post("/api/lotes/{id}/reintentar", loteId));
    }

    private void cuandoSeReintentaSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post("/api/lotes/{id}/reintentar", loteId).session(sesion.sesion()));
    }

    private void cuandoSeReintenta() throws Exception {
        respuesta = mockMvc.perform(post("/api/lotes/{id}/reintentar", loteId)
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    private void cuandoSeConsultaLaLectura() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}/lectura", loteId).session(sesion.sesion()));
    }

    private void cuandoSeConsultaUnaLecturaConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/invalido/lectura").session(sesion.sesion()));
    }

    private void cuandoSeConsultaElConsumoConOtroNegocio() throws Exception {
        respuesta = mockMvc.perform(get("/api/ia/consumo").session(sesion.sesion())
                .param("negocioId", UUID.randomUUID().toString()));
    }

    // --- entonces ---

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    private void entoncesLaLecturaEstaPendienteYNoSeProceso() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.loteId").value(loteId.toString()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.intentos").value(0))
                .andExpect(jsonPath("$.propuesta").doesNotExist());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM lectura_ia WHERE lote_id = ?",
                Integer.class, loteId.valor())).isZero();
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesSeDevuelveLaPropuestaCompletaYTipada() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO"))
                .andExpect(jsonPath("$.propuesta.tipoRegistro").value("VENTAS"))
                .andExpect(jsonPath("$.propuesta.nombre").value("Ventas"))
                .andExpect(jsonPath("$.propuesta.campos[0].clave").value("monto"))
                .andExpect(jsonPath("$.propuesta.campos[0].nombre").value("Monto"))
                .andExpect(jsonPath("$.propuesta.campos[0].tipo").value("NUMERO"))
                .andExpect(jsonPath("$.propuesta.campos[0].opciones").isEmpty())
                .andExpect(jsonPath("$.propuesta.filas[0].pagina").value(1))
                .andExpect(jsonPath("$.propuesta.filas[0].renglon").value(2))
                .andExpect(jsonPath("$.propuesta.filas[0].datos.monto.valor").value("100"))
                .andExpect(jsonPath("$.propuesta.filas[0].datos.monto.confianza").value(0.9))
                .andExpect(jsonPath("$.propuesta.mapeoCampos.monto").value(ProcesamientoTestData.CAMPO_ID.toString()))
                .andExpect(jsonPath("$.propuesta.advertencias[0]").value("Aviso"))
                .andExpect(jsonPath("$.propuesta.consumo.proveedor").value("simulado"))
                .andExpect(jsonPath("$.propuesta.consumo.modelo").value("demo"))
                .andExpect(jsonPath("$.propuesta.consumo.versionPrompt").value("v1"))
                .andExpect(jsonPath("$.propuesta.consumo.paginas").value(1))
                .andExpect(jsonPath("$.propuesta.consumo.tokensEntrada").value(10))
                .andExpect(jsonPath("$.propuesta.consumo.tokensSalida").value(20))
                .andExpect(jsonPath("$.propuesta.consumo.duracionMs").value(30));
    }

    private void entoncesSeAceptaYElLoteVuelveAPendiente() throws Exception {
        respuesta.andExpect(status().isAccepted());
        assertThat(procesamientoRepository.obtener(negocioId, loteId).orElseThrow().estado().name())
                .isEqualTo("PENDIENTE");
    }

    private void entoncesSeRechazaPorqueSoloSeReintentaUnLoteEnError() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sólo se puede reintentar un lote en estado ERROR"));
    }

    private void entoncesSeDevuelveElConsumoDelNegocioDeLaSesion() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.limite").value(100))
                .andExpect(jsonPath("$.paginas").value(5))
                .andExpect(jsonPath("$.reservadas").value(2))
                .andExpect(jsonPath("$.disponibles").value(93))
                .andExpect(jsonPath("$.tokensEntrada").value(0))
                .andExpect(jsonPath("$.tokensSalida").value(0))
                .andExpect(jsonPath("$.consumoCompleto").value(true));
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    // --- datos ---

    private void borrarLaOtraCuenta() {
        if (otroNegocio == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", otroNegocio);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", otroUsuario);
    }
}
