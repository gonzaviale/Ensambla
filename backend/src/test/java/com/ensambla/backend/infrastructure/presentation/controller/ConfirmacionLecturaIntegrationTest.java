package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfirmacionLecturaIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String CONFIRMAR = "/api/lotes/{id}/confirmar";

    private static final String CUERPO_NUEVO = """
            {
              "nombreProyecto": "Ventas",
              "nombreBloque": "Hoja 1",
              "campos": [
                {"clave": "monto", "nombre": "Monto", "tipo": "NUMERO"},
                {"clave": "pagado", "nombre": "Pagado", "tipo": "SI_NO"}
              ],
              "filas": [
                {"pagina": 1, "valores": {"monto": 850, "pagado": true}},
                {"pagina": null, "valores": {"monto": 100}}
              ]
            }
            """;

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
    private ArchivoCuadernoPort archivoCuadernoPort;

    private MockMvc mockMvc;
    private UUID usuarioId;
    private UUID negocioId;
    private UUID loteId;
    private UUID fotoId;
    private SesionHttpDePrueba sesion;
    private final List<String> clavesGuardadas = new ArrayList<>();
    private ResultActions respuesta;
    private String bloqueConfirmadoId;
    private String proyectoConfirmadoId;
    private UUID campoMontoId;
    private UUID otroLoteId;
    private UUID proyectoExistenteId;
    private UUID otroUsuarioId;
    private UUID otroNegocioId;
    private UUID registroId;
    private List<Map<String, Object>> registrosAntes;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Negocio', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(NegocioId.de(negocioId), OffsetDateTime.now().minusDays(1));
        fotoId = UUID.randomUUID();
        loteId = crearLote("LISTO", fotoId);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @AfterEach
    void limpiarLasCuentas() {
        clavesGuardadas.forEach(clave -> archivoCuadernoPort.eliminar(NegocioId.de(negocioId), clave));
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
        borrarLaOtraCuenta();
    }

    @Test
    @DisplayName("Confirmar sin sesión devuelve 401")
    void confirmarSinSesionDevuelve401() throws Exception {
        cuandoSeConfirmaSinSesion();

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Confirmar sin CSRF devuelve 403")
    void confirmarSinCsrfDevuelve403() throws Exception {
        cuandoSeConfirmaSinCsrf();

        entoncesSeProhibe();
    }

    @Test
    @DisplayName("Crea proyecto, tabla, columnas y registros con su foto y deja el lote confirmado")
    void creaProyectoTablaColumnasYRegistrosConSuFotoYDejaElLoteConfirmado() throws Exception {
        cuandoSeConfirmaElLote(CUERPO_NUEVO);

        entoncesSeCreaTodoYElLoteQuedaConfirmado();
    }

    @Test
    @DisplayName("Rechaza por cuota todas las filas de una foto y revierte la estructura recién creada")
    void rechazaPorCuotaTodasLasFilasDeUnaFotoYRevierteLaEstructuraRecienCreada() throws Exception {
        dadoUnProyectoConElLimiteDeRegistrosCasiAlcanzado();

        cuandoSeConfirmaElLote(CUERPO_NUEVO.replace("\"nombreProyecto\": \"Ventas\"",
                "\"proyectoId\": \"" + proyectoExistenteId + "\""));

        entoncesSeRechazaPorCuotaYSeRevierteLaEstructura();
    }

    @Test
    @DisplayName("Después de confirmar la lectura del lote figura como confirmada")
    void despuesDeConfirmarLaLecturaDelLoteFiguraComoConfirmada() throws Exception {
        dadoUnLoteConfirmado();

        cuandoSeConsultaLaLecturaDelLote();

        entoncesLaLecturaFiguraComoConfirmada();
    }

    @Test
    @DisplayName("Confirmar dos veces el mismo lote responde 400 y no duplica nada")
    void confirmarDosVecesElMismoLoteResponde400YNoDuplicaNada() throws Exception {
        dadoUnLoteConfirmado();

        cuandoSeConfirmaElLote(CUERPO_NUEVO);

        entoncesSeRechazaLaSegundaConfirmacionSinDuplicarNada();
    }

    @Test
    @DisplayName("Si una fila es inválida no queda proyecto, tabla ni registros y el lote sigue listo")
    void siUnaFilaEsInvalidaNoQuedaProyectoTablaNiRegistrosYElLoteSigueListo() throws Exception {
        cuandoSeConfirmaElLote(CUERPO_NUEVO.replace("\"monto\": 100", "\"monto\": \"mucho\""));

        entoncesSeRevierteTodoPorLaFilaInvalida();
    }

    @Test
    @DisplayName("Suma las filas de otro lote a la tabla ya creada reutilizando su columna")
    void sumaLasFilasDeOtroLoteALaTablaYaCreadaReutilizandoSuColumna() throws Exception {
        dadoUnLoteConfirmadoYOtroListoParaSumar();

        cuandoSeConfirmaOtroLote(cuerpoQueSumaUnaFilaALaTabla());

        entoncesSeSumaUnaFilaALaTablaExistente();
    }

    @Test
    @DisplayName("Crea la tabla dentro de un proyecto existente")
    void creaLaTablaDentroDeUnProyectoExistente() throws Exception {
        dadoUnLoteConfirmadoYOtroListoParaSumar();

        cuandoSeConfirmaOtroLote(cuerpoQueCreaUnaTablaEnElProyectoConfirmado());

        entoncesSeCreaLaSegundaTablaEnElMismoProyecto();
    }

    @Test
    @DisplayName("Un lote que todavía no terminó de leerse no se puede confirmar")
    void unLoteQueTodaviaNoTerminoDeLeerseNoSePuedeConfirmar() throws Exception {
        dadoUnLotePendiente();

        cuandoSeConfirmaOtroLote(CUERPO_NUEVO);

        entoncesSeRechazaPorqueTodaviaNoTerminoDeLeerse();
    }

    @Test
    @DisplayName("Confirmar un lote de otro negocio devuelve 404 y no crea nada")
    void confirmarUnLoteDeOtroNegocioDevuelve404YNoCreaNada() throws Exception {
        dadoUnaSesionDeOtroNegocio();

        cuandoSeConfirmaElLote(CUERPO_NUEVO);

        entoncesRespondeNoEncontradoYNoSeCreaNada();
    }

    @Test
    @DisplayName("Sirve la foto de un lote del negocio con su tipo")
    void sirveLaFotoDeUnLoteDelNegocioConSuTipo() throws Exception {
        cuandoSePideLaFotoDelLote(fotoId);

        entoncesSeSirveLaFotoConSuTipo();
    }

    @Test
    @DisplayName("Pedir una foto sin sesión devuelve 401")
    void pedirUnaFotoSinSesionDevuelve401() throws Exception {
        cuandoSePideLaFotoDelLoteSinSesion();

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Una foto que no es del lote devuelve 404")
    void unaFotoQueNoEsDelLoteDevuelve404() throws Exception {
        cuandoSePideLaFotoDelLote(UUID.randomUUID());

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("La foto de un lote de otro negocio devuelve 404")
    void laFotoDeUnLoteDeOtroNegocioDevuelve404() throws Exception {
        dadoUnaSesionDeOtroNegocio();

        cuandoSePideLaFotoDelLote(fotoId);

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un registro confirmado expone su foto de origen y se puede ver")
    void unRegistroConfirmadoExponeSuFotoDeOrigenYSePuedeVer() throws Exception {
        dadoUnLoteConfirmado();
        dadoUnRegistroConFoto();

        cuandoSePideElRegistro();

        entoncesElRegistroExponeSuFotoDeOrigenYSePuedeVer();
    }

    @Test
    @DisplayName("Un registro cargado a mano no tiene foto de origen y devuelve 404")
    void unRegistroCargadoAManoNoTieneFotoDeOrigenYDevuelve404() throws Exception {
        dadoUnLoteConfirmado();
        dadoUnRegistroCargadoAMano();

        cuandoSePideElRegistro();

        entoncesElRegistroNoTieneFotoDeOrigen();
    }

    @Test
    @DisplayName("La foto de origen de un registro de otro negocio devuelve 404")
    void laFotoDeOrigenDeUnRegistroDeOtroNegocioDevuelve404() throws Exception {
        dadoUnLoteConfirmado();
        dadoUnRegistroConFoto();
        dadoUnaSesionDeOtroNegocio();

        cuandoSePideLaFotoDeOrigenDelRegistro();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un body sin columnas devuelve 400")
    void unBodySinColumnasDevuelve400() throws Exception {
        cuandoSeConfirmaElLote("{\"nombreProyecto\": \"Ventas\", \"nombreBloque\": \"Hoja\", \"filas\": []}");

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("Suma las filas a una tabla existente, le agrega la columna nueva y no toca los registros previos")
    void sumaLasFilasAUnaTablaExistenteLeAgregaLaColumnaNuevaYNoTocaLosRegistrosPrevios() throws Exception {
        dadoUnaTablaConfirmadaConSusRegistros();

        cuandoSeConfirmaOtroLote(cuerpoConTelefono("40", "11 4444-5555"));

        entoncesSeAgregaLaColumnaNuevaYLasFilasSinTocarLosRegistrosPrevios();
    }

    @Test
    @DisplayName("Si una fila es inválida no queda la columna nueva ni los registros y el lote sigue listo")
    void siUnaFilaEsInvalidaNoQuedaLaColumnaNuevaNiLosRegistrosYElLoteSigueListo() throws Exception {
        dadoUnaTablaConfirmadaConSusRegistros();

        cuandoSeConfirmaOtroLote(cuerpoConTelefono("\"mucho\"", "11 4444-5555"));

        entoncesSeRevierteLaColumnaNuevaPorLaFilaInvalida();
    }

    @Test
    @DisplayName("Si una columna nueva repite el nombre de un campo no se crea ninguna de las nuevas")
    void siUnaColumnaNuevaRepiteElNombreDeUnCampoNoSeCreaNingunaDeLasNuevas() throws Exception {
        dadoUnaTablaConfirmadaConSusRegistros();

        cuandoSeConfirmaOtroLote(cuerpoConUnNombreRepetido());

        entoncesNoSeCreaNingunaColumnaNuevaPorElNombreRepetido();
    }

    @Test
    @DisplayName("Dos columnas que apuntan al mismo campo de la tabla se rechazan y no se guarda nada")
    void dosColumnasQueApuntanAlMismoCampoDeLaTablaSeRechazanYNoSeGuardaNada() throws Exception {
        dadoUnaTablaConfirmadaConSusRegistros();

        cuandoSeConfirmaOtroLote(cuerpoConDosColumnasParaElMismoCampo());

        entoncesSeRechazanLasColumnasQueApuntanAlMismoCampo();
    }

    // --- dado ---

    private void dadoUnLoteConfirmado() throws Exception {
        String cuerpo = confirmar(loteId, CUERPO_NUEVO).andReturn().getResponse().getContentAsString();
        bloqueConfirmadoId = JsonPath.read(cuerpo, "$.bloqueId");
        proyectoConfirmadoId = JsonPath.read(cuerpo, "$.proyectoId");
        campoMontoId = jdbcTemplate.queryForObject(
                "SELECT id FROM campo WHERE bloque_id = ?::uuid AND nombre = 'Monto'", UUID.class,
                bloqueConfirmadoId);
    }

    private void dadoUnLoteConfirmadoYOtroListoParaSumar() throws Exception {
        dadoUnLoteConfirmado();
        otroLoteId = crearLote("LISTO", UUID.randomUUID());
    }

    private void dadoUnaTablaConfirmadaConSusRegistros() throws Exception {
        dadoUnLoteConfirmadoYOtroListoParaSumar();
        registrosAntes = registrosDe(bloqueConfirmadoId);
    }

    private void dadoUnLotePendiente() {
        otroLoteId = crearLote("PENDIENTE", UUID.randomUUID());
    }

    private void dadoUnProyectoConElLimiteDeRegistrosCasiAlcanzado() {
        proyectoExistenteId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Sistema existente')",
                proyectoExistenteId, negocioId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Inicial')",
                bloqueId, negocioId, proyectoExistenteId);
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, 999)
                """, negocioId, bloqueId);
    }

    private void dadoUnaSesionDeOtroNegocio() throws Exception {
        otroUsuarioId = UUID.randomUUID();
        otroNegocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Otra persona')",
                otroUsuarioId, otroUsuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Otro', ?)",
                otroNegocioId, otroUsuarioId);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, otroUsuarioId.toString());
    }

    private void dadoUnRegistroConFoto() {
        registroId = jdbcTemplate.queryForObject(
                "SELECT id FROM registro WHERE negocio_id = ? AND foto_origen_id = ?", UUID.class, negocioId, fotoId);
    }

    private void dadoUnRegistroCargadoAMano() {
        registroId = jdbcTemplate.queryForObject(
                "SELECT id FROM registro WHERE negocio_id = ? AND foto_origen_id IS NULL", UUID.class, negocioId);
    }

    // --- cuando ---

    private void cuandoSeConfirmaElLote(String cuerpo) throws Exception {
        respuesta = confirmar(loteId, cuerpo);
    }

    private void cuandoSeConfirmaOtroLote(String cuerpo) throws Exception {
        respuesta = confirmar(otroLoteId, cuerpo);
    }

    private void cuandoSeConfirmaSinSesion() throws Exception {
        respuesta = mockMvc.perform(post(CONFIRMAR, loteId)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_NUEVO));
    }

    private void cuandoSeConfirmaSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post(CONFIRMAR, loteId).session(sesion.sesion())
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_NUEVO));
    }

    private void cuandoSeConsultaLaLecturaDelLote() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}/lectura", loteId).session(sesion.sesion()));
    }

    private void cuandoSePideLaFotoDelLote(UUID foto) throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", loteId, foto).session(sesion.sesion()));
    }

    private void cuandoSePideLaFotoDelLoteSinSesion() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", loteId, fotoId));
    }

    private void cuandoSePideElRegistro() throws Exception {
        respuesta = mockMvc.perform(get("/api/registros/{id}", registroId).session(sesion.sesion()));
    }

    private void cuandoSePideLaFotoDeOrigenDelRegistro() throws Exception {
        respuesta = mockMvc.perform(get("/api/registros/{id}/foto-origen", registroId).session(sesion.sesion()));
    }

    // --- entonces ---

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesSeCreaTodoYElLoteQuedaConfirmado() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.registros").value(2))
                .andExpect(jsonPath("$.proyectoId").isNotEmpty())
                .andExpect(jsonPath("$.bloqueId").isNotEmpty());
        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("bloque")).isEqualTo(1);
        assertThat(contar("campo")).isEqualTo(2);
        assertThat(contar("registro")).isEqualTo(2);
        assertThat(contarConFoto(fotoId)).isEqualTo(1);
        assertThat(estadoDelLote(loteId)).isEqualTo("CONFIRMADO");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT confirmado_en IS NOT NULL AND bloque_destino_id IS NOT NULL FROM lote_subida WHERE id = ?",
                Boolean.class, loteId)).isTrue();
    }

    private void entoncesSeRechazaPorCuotaYSeRevierteLaEstructura() throws Exception {
        respuesta.andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_REGISTROS"));
        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("bloque")).isEqualTo(1);
        assertThat(contar("campo")).isZero();
        assertThat(contar("registro")).isEqualTo(999);
        assertThat(estadoDelLote(loteId)).isEqualTo("LISTO");
    }

    private void entoncesLaLecturaFiguraComoConfirmada() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    private void entoncesSeRechazaLaSegundaConfirmacionSinDuplicarNada() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este cuaderno ya se guardó"));
        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("registro")).isEqualTo(2);
    }

    private void entoncesSeRevierteTodoPorLaFilaInvalida() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['filas[1].monto']").value("Tiene que ser un número"));
        assertThat(contar("proyecto")).isZero();
        assertThat(contar("bloque")).isZero();
        assertThat(contar("campo")).isZero();
        assertThat(contar("registro")).isZero();
        assertThat(estadoDelLote(loteId)).isEqualTo("LISTO");
    }

    private void entoncesSeSumaUnaFilaALaTablaExistente() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueId").value(bloqueConfirmadoId))
                .andExpect(jsonPath("$.registros").value(1));
        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("registro")).isEqualTo(3);
        assertThat(estadoDelLote(otroLoteId)).isEqualTo("CONFIRMADO");
    }

    private void entoncesSeCreaLaSegundaTablaEnElMismoProyecto() throws Exception {
        respuesta.andExpect(status().isCreated()).andExpect(jsonPath("$.proyectoId").value(proyectoConfirmadoId));
        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("bloque")).isEqualTo(2);
    }

    private void entoncesSeRechazaPorqueTodaviaNoTerminoDeLeerse() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este cuaderno todavía no terminó de leerse"));
    }

    private void entoncesRespondeNoEncontradoYNoSeCreaNada() throws Exception {
        respuesta.andExpect(status().isNotFound());
        assertThat(contar("proyecto")).isZero();
    }

    private void entoncesSeSirveLaFotoConSuTipo() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG));
    }

    private void entoncesElRegistroExponeSuFotoDeOrigenYSePuedeVer() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.fotoOrigenId").value(fotoId.toString()));
        mockMvc.perform(get("/api/registros/{id}/foto-origen", registroId).session(sesion.sesion()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG));
    }

    private void entoncesElRegistroNoTieneFotoDeOrigen() throws Exception {
        respuesta.andExpect(jsonPath("$.fotoOrigenId").isEmpty());
        mockMvc.perform(get("/api/registros/{id}/foto-origen", registroId).session(sesion.sesion()))
                .andExpect(status().isNotFound());
    }

    private void entoncesSeAgregaLaColumnaNuevaYLasFilasSinTocarLosRegistrosPrevios() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueId").value(bloqueConfirmadoId))
                .andExpect(jsonPath("$.registros").value(2));
        assertThat(contar("campo")).isEqualTo(3);
        UUID telefono = jdbcTemplate.queryForObject(
                "SELECT id FROM campo WHERE bloque_id = ?::uuid AND nombre = 'Teléfono'", UUID.class,
                bloqueConfirmadoId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT posicion FROM campo WHERE id = ?", Integer.class, telefono)).isEqualTo(2);
        assertThat(contar("registro")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM registro WHERE bloque_id = ?::uuid AND valores ->> ? = '11 4444-5555'
                """, Integer.class, bloqueConfirmadoId, telefono.toString())).isEqualTo(2);
        assertThat(registrosDe(bloqueConfirmadoId).stream().filter(registrosAntes::contains).toList())
                .containsExactlyInAnyOrderElementsOf(registrosAntes);
        assertThat(estadoDelLote(otroLoteId)).isEqualTo("CONFIRMADO");
    }

    private void entoncesSeRevierteLaColumnaNuevaPorLaFilaInvalida() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['filas[1].monto']").value("Tiene que ser un número"));
        assertThat(contar("campo")).isEqualTo(2);
        assertThat(registrosDe(bloqueConfirmadoId)).containsExactlyInAnyOrderElementsOf(registrosAntes);
        assertThat(estadoDelLote(otroLoteId)).isEqualTo("LISTO");
    }

    private void entoncesNoSeCreaNingunaColumnaNuevaPorElNombreRepetido() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['campos[2].nombre']")
                        .value("Ya hay un campo llamado \"monto\" en este bloque"));
        assertThat(contar("campo")).isEqualTo(2);
        assertThat(contar("registro")).isEqualTo(2);
        assertThat(estadoDelLote(otroLoteId)).isEqualTo("LISTO");
    }

    private void entoncesSeRechazanLasColumnasQueApuntanAlMismoCampo() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Dos columnas no pueden guardarse en el mismo campo de la tabla"));
        assertThat(contar("registro")).isEqualTo(2);
        assertThat(estadoDelLote(otroLoteId)).isEqualTo("LISTO");
    }

    // --- datos ---

    private ResultActions confirmar(UUID lote, String cuerpo) throws Exception {
        return mockMvc.perform(post(CONFIRMAR, lote).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String cuerpoQueSumaUnaFilaALaTabla() {
        return """
                {
                  "bloqueId": "%s",
                  "campos": [{"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"}],
                  "filas": [{"pagina": 1, "valores": {"monto": 40}}]
                }
                """.formatted(bloqueConfirmadoId, campoMontoId);
    }

    private String cuerpoQueCreaUnaTablaEnElProyectoConfirmado() {
        return """
                {
                  "proyectoId": "%s",
                  "nombreBloque": "Hoja 2",
                  "campos": [{"clave": "nota", "nombre": "Nota", "tipo": "TEXTO"}],
                  "filas": [{"pagina": 1, "valores": {"nota": "hola"}}]
                }
                """.formatted(proyectoConfirmadoId);
    }

    /** Dos filas que suman a la tabla y traen la columna nueva «Teléfono» (el monto de la segunda se pasa tal cual). */
    private String cuerpoConTelefono(String montoSegundaFila, String telefono) {
        return """
                {
                  "bloqueId": "%s",
                  "campos": [
                    {"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"},
                    {"clave": "telefono", "nombre": "Teléfono", "tipo": "TEXTO"}
                  ],
                  "filas": [
                    {"pagina": 1, "valores": {"monto": 20, "telefono": "%s"}},
                    {"pagina": 1, "valores": {"monto": %s, "telefono": "%s"}}
                  ]
                }
                """.formatted(bloqueConfirmadoId, campoMontoId, telefono, montoSegundaFila, telefono);
    }

    private String cuerpoConUnNombreRepetido() {
        return """
                {
                  "bloqueId": "%s",
                  "campos": [
                    {"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"},
                    {"clave": "telefono", "nombre": "Teléfono", "tipo": "TEXTO"},
                    {"clave": "importe", "nombre": " monto ", "tipo": "NUMERO"}
                  ],
                  "filas": [{"pagina": 1, "valores": {"monto": 40, "telefono": "1", "importe": 5}}]
                }
                """.formatted(bloqueConfirmadoId, campoMontoId);
    }

    private String cuerpoConDosColumnasParaElMismoCampo() {
        return """
                {
                  "bloqueId": "%s",
                  "campos": [
                    {"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"},
                    {"clave": "importe", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"}
                  ],
                  "filas": [{"pagina": 1, "valores": {"monto": 40, "importe": 5}}]
                }
                """.formatted(bloqueConfirmadoId, campoMontoId, campoMontoId);
    }

    private UUID crearLote(String estado, UUID foto) {
        UUID id = UUID.randomUUID();
        String clave = negocioId + "/fotos/" + foto + ".jpg";
        jdbcTemplate.update("INSERT INTO lote_subida (id, negocio_id, estado) VALUES (?, ?, ?)", id, negocioId, estado);
        jdbcTemplate.update("""
                INSERT INTO foto_cuaderno (id, negocio_id, lote_id, clave_almacenamiento, tipo_contenido,
                                           tamanio_bytes, posicion)
                VALUES (?, ?, ?, ?, 'image/jpeg', ?, 0)
                """, foto, negocioId, id, clave, JPEG.length);
        archivoCuadernoPort.guardar(NegocioId.de(negocioId), clave, JPEG);
        clavesGuardadas.add(clave);
        return id;
    }

    private void borrarLaOtraCuenta() {
        if (otroNegocioId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", otroNegocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", otroUsuarioId);
    }

    private int contar(String tabla) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + tabla + " WHERE negocio_id = ?", Integer.class, negocioId);
    }

    private List<Map<String, Object>> registrosDe(String bloqueId) {
        return jdbcTemplate.queryForList(
                "SELECT id::text AS id, valores::text AS valores, actualizado_en::text AS actualizado_en "
                        + "FROM registro WHERE bloque_id = ?::uuid ORDER BY id", bloqueId);
    }

    private int contarConFoto(UUID foto) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM registro WHERE foto_origen_id = ?", Integer.class, foto);
    }

    private String estadoDelLote(UUID lote) {
        return jdbcTemplate.queryForObject("SELECT estado FROM lote_subida WHERE id = ?", String.class, lote);
    }
}
