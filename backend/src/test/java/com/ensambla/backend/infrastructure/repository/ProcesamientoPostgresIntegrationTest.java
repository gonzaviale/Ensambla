package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.service.procesamiento.ConexionesLecturaService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import com.ensambla.backend.domain.service.procesamiento.ReservaIaService;
import com.ensambla.backend.domain.useCase.procesamiento.procesarLote.ProcesarLote;
import com.ensambla.backend.infrastructure.ia.ArchivoCuadernoLocalAdapter;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.ProcesamientoRepositoryImpl;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class ProcesamientoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    @TempDir
    private Path archivos;

    @Autowired
    private LectorCuadernoPort lectorCuadernoPort;

    @Autowired
    private ProcesamientoRepository procesamientoRepository;

    @Autowired
    private ReservaIaService reservaIaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SuscripcionService suscripcionService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID usuarioId;
    private UUID negocioId;

    @Test
    @DisplayName("Dos lotes del mismo cuaderno recorren el worker real y reutilizan el bloque propio")
    void executeShouldProcessBothLotsIntoSameExistingBlock() throws Exception {
        UUID primero = givenLote();
        UUID segundo = givenLote();
        UUID bloqueId = givenBloqueSimulado();
        givenFoto(primero, 0, "primera.jpg");
        givenFoto(segundo, 0, "segunda.jpg");
        Files.createDirectory(archivos.resolve(negocioId.toString()));
        Files.write(archivos.resolve(negocioId + "/primera.jpg"), new byte[]{1, 2, 3});
        Files.write(archivos.resolve(negocioId + "/segunda.jpg"), new byte[]{4, 5, 6});
        var procesamientoService = new ProcesamientoService(procesamientoRepository,
                new ArchivoCuadernoLocalAdapter(archivos, 10), lectorCuadernoPort,
                new PropuestaLecturaService(), new ConexionesLecturaService(null, null),
                ProcesamientoTestData.politica(), reservaIaService);
        var useCase = new ProcesarLote(procesamientoService, Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

        useCase.execute();
        useCase.execute();

        var lecturaPrimera = procesamientoRepository.obtener(negocioId, primero).orElseThrow();
        var lecturaSegunda = procesamientoRepository.obtener(negocioId, segundo).orElseThrow();
        assertThat(lecturaPrimera.estado()).isEqualTo(EstadoLote.LISTO);
        assertThat(lecturaSegunda.estado()).isEqualTo(EstadoLote.LISTO);
        assertThat(lecturaPrimera.propuesta().lectura().bloque().bloqueExistenteId()).isEqualTo(bloqueId);
        assertThat(lecturaSegunda.propuesta().lectura().bloque().bloqueExistenteId()).isEqualTo(bloqueId);
        assertThat(lecturaPrimera.propuesta().mapeoCampos()).hasSize(6);
        assertThat(lecturaSegunda.propuesta().mapeoCampos()).isEqualTo(lecturaPrimera.propuesta().mapeoCampos());
        assertThat(procesamientoRepository.consumo(negocioId, AHORA.toLocalDate()).paginas()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?",
                Integer.class, negocioId)).isZero();
    }

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba IA')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba IA', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(negocioId, AHORA.minusDays(1));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("Dos workers reclaman lotes distintos sin perder trabajo")
    void reclamarShouldClaimDifferentLotsConcurrently() throws Exception {
        givenLote();
        givenLote();

        List<Optional<LoteTrabajo>> resultados = concurrently(
                () -> procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2)));

        assertThat(resultados).allMatch(Optional::isPresent);
        assertThat(resultados.getFirst().orElseThrow().id()).isNotEqualTo(resultados.getLast().orElseThrow().id());
        assertThat(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).isEmpty();
    }

    @Test
    @DisplayName("Dos reservas concurrentes no superan el límite diario")
    void reservarShouldEnforceQuotaConcurrently() throws Exception {
        givenLote();
        givenLote();
        List<LoteTrabajo> trabajos = List.of(claim(), claim());
        var indice = new AtomicInteger();

        List<Boolean> resultados = concurrently(() ->
                procesamientoRepository.reservar(trabajos.get(indice.getAndIncrement()), 1, 1, AHORA));

        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        ConsumoDiario consumo = procesamientoRepository.consumo(negocioId, AHORA.toLocalDate());
        assertThat(consumo.reservadas()).isEqualTo(1);
        assertThat(consumo.paginas()).isZero();
    }

    @Test
    @DisplayName("No reserva por encima del límite aun sin historial")
    void reservarShouldRejectOversizedInitialReservation() {
        givenLote();
        LoteTrabajo lote = claim();

        boolean reservado = procesamientoRepository.reservar(lote, 2, 1, AHORA);

        assertThat(reservado).isFalse();
        assertThat(procesamientoRepository.consumo(negocioId, AHORA.toLocalDate()).reservadas()).isZero();
    }

    @Test
    @DisplayName("Un token ajeno no puede reservar consumo")
    void reservarShouldRejectForeignToken() {
        givenLote();
        LoteTrabajo real = claim();
        LoteTrabajo falso = new LoteTrabajo(real.id(), negocioId, null, UUID.randomUUID(), 1);

        boolean reservado = procesamientoRepository.reservar(falso, 1, 10, AHORA);

        assertThat(reservado).isFalse();
    }

    @Test
    @DisplayName("Una reserva no se contabiliza dos veces")
    void reservarShouldRejectDuplicateReservation() {
        givenLote();
        LoteTrabajo lote = claim();
        assertThat(procesamientoRepository.reservar(lote, 1, 10, AHORA)).isTrue();

        boolean reservado = procesamientoRepository.reservar(lote, 1, 10, AHORA);

        assertThat(reservado).isFalse();
        assertThat(procesamientoRepository.consumo(negocioId, AHORA.toLocalDate()).reservadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Persiste propuesta y consumo una sola vez aunque se repita la finalización")
    void finalizarShouldPersistExactlyOnce() {
        UUID loteId = givenLote();
        LoteTrabajo lote = claim();
        procesamientoRepository.reservar(lote, 1, 100, AHORA);
        ResultadoIntento exito = new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false);

        procesamientoRepository.finalizar(lote, exito, BigDecimal.ZERO, AHORA, null);
        procesamientoRepository.finalizar(lote, exito, BigDecimal.ZERO, AHORA, null);

        var estado = procesamientoRepository.obtener(negocioId, loteId).orElseThrow();
        assertThat(estado.estado()).isEqualTo(EstadoLote.LISTO);
        assertThat(estado.intentos()).isEqualTo(1);
        assertThat(estado.propuesta().mapeoCampos()).containsEntry("monto", ProcesamientoTestData.CAMPO_ID);
        assertThat(estado.propuesta().lectura().filas()).hasSize(1);
        ConsumoDiario consumo = procesamientoRepository.consumo(negocioId, AHORA.toLocalDate());
        assertThat(consumo.paginas()).isEqualTo(1);
        assertThat(consumo.reservadas()).isZero();
        assertThat(consumo.tokensEntrada()).isEqualTo(10);
        assertThat(consumo.tokensSalida()).isEqualTo(20);
        assertThat(consumo.costoEstimadoUsd()).isEqualByComparingTo("0");
        assertThat(consumo.consumoCompleto()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM lectura_ia WHERE lote_id = ?",
                Integer.class, loteId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Un intento que cruza medianoche conserva el día de su reserva")
    void finalizarShouldKeepReservationDateAcrossMidnight() {
        givenLote();
        LoteTrabajo lote = claim();
        procesamientoRepository.reservar(lote, 1, 100, AHORA);

        procesamientoRepository.finalizar(lote, new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false),
                BigDecimal.ZERO, AHORA.plusDays(1), null);

        assertThat(procesamientoRepository.consumo(negocioId, AHORA.toLocalDate()).tokensEntrada()).isEqualTo(10);
        assertThat(procesamientoRepository.consumo(negocioId, AHORA.plusDays(1).toLocalDate()).paginas()).isZero();
    }

    @Test
    @DisplayName("Recupera reclamo vencido y descarta un resultado tardío")
    void finalizarShouldFenceLateResultsAfterRecovery() {
        UUID loteId = givenLote();
        LoteTrabajo viejo = claim();
        procesamientoRepository.reservar(viejo, 1, 100, AHORA);
        assertThat(procesamientoRepository.vencidos(AHORA.plusMinutes(2))).contains(viejo);
        procesamientoRepository.finalizar(viejo, new ResultadoIntento(null, "Timeout", true),
                null, AHORA.plusMinutes(2), AHORA.plusMinutes(3));
        assertThat(procesamientoRepository.reclamar(AHORA.plusMinutes(2), AHORA.plusMinutes(4))).isEmpty();
        LoteTrabajo nuevo = procesamientoRepository.reclamar(AHORA.plusMinutes(3), AHORA.plusMinutes(5)).orElseThrow();

        procesamientoRepository.finalizar(viejo, new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false),
                BigDecimal.ZERO, AHORA.plusMinutes(3), null);

        assertThat(procesamientoRepository.obtener(negocioId, loteId).orElseThrow().estado())
                .isEqualTo(EstadoLote.PROCESANDO);
        assertThat(nuevo.reclamoId()).isNotEqualTo(viejo.reclamoId());
        assertThat(nuevo.intentos()).isEqualTo(2);
        assertThat(procesamientoRepository.consumo(negocioId, AHORA.toLocalDate()).consumoCompleto()).isFalse();
    }

    @Test
    @DisplayName("Los errores anteriores al proveedor no consumen páginas")
    void finalizarShouldNotBillUnreservedWork() {
        UUID loteId = givenLote();
        LoteTrabajo lote = claim();

        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Foto inválida", false),
                null, AHORA, null);

        var estado = procesamientoRepository.obtener(negocioId, loteId).orElseThrow();
        assertThat(estado.estado()).isEqualTo(EstadoLote.ERROR);
        assertThat(estado.motivoError()).isEqualTo("Foto inválida");
        assertThat(estado.propuesta()).isNull();
        ConsumoDiario consumo = procesamientoRepository.consumo(negocioId, AHORA.toLocalDate());
        assertThat(consumo.paginas()).isZero();
        assertThat(consumo.consumoCompleto()).isTrue();
        assertThat(consumo.costoEstimadoUsd()).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"modelo-real"})
    @DisplayName("Registra una falla con consumo desconocido y el modelo configurado")
    void finalizarShouldRecordUnknownProviderUsage(String modelo) {
        givenLote();
        LoteTrabajo lote = claim();
        procesamientoRepository.reservar(lote, 1, 100, AHORA);
        var repository = new ProcesamientoRepositoryImpl(namedParameterJdbcTemplate, objectMapper,
                new IaProperties("prueba", modelo, Duration.ofSeconds(1), 10, 0.8, null));

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                repository.finalizar(lote, new ResultadoIntento(null, "Timeout", false), null, AHORA, null));

        ConsumoDiario consumo = procesamientoRepository.consumo(negocioId, AHORA.toLocalDate());
        assertThat(consumo.paginas()).isEqualTo(1);
        assertThat(consumo.costoEstimadoUsd()).isNull();
        assertThat(consumo.consumoCompleto()).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT proveedor FROM lectura_ia WHERE lote_id = ?",
                String.class, lote.id())).isEqualTo("prueba");
    }

    @Test
    @DisplayName("Reencola sólo ERROR del negocio propio y reinicia el contador")
    void reencolarShouldOnlyRequeueOwnErrors() {
        UUID loteId = givenLote();
        LoteTrabajo lote = claim();
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Error", false), null, AHORA, null);
        assertThat(procesamientoRepository.reencolar(UUID.randomUUID(), loteId, AHORA)).isFalse();

        boolean modificado = procesamientoRepository.reencolar(negocioId, loteId, AHORA);

        assertThat(modificado).isTrue();
        assertThat(procesamientoRepository.obtener(negocioId, loteId).orElseThrow().intentos()).isZero();
        assertThat(procesamientoRepository.reencolar(negocioId, loteId, AHORA)).isFalse();
        assertThat(procesamientoRepository.obtener(UUID.randomUUID(), loteId)).isEmpty();
    }

    @Test
    @DisplayName("Carga fotos ordenadas y contexto tipado del negocio sin mezclar datos")
    void fotosAndBloquesShouldIsolateAndOrderContext() {
        UUID loteId = givenLote();
        UUID proyectoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        UUID campoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Proyecto')",
                proyectoId, negocioId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Ventas')",
                bloqueId, negocioId, proyectoId);
        jdbcTemplate.update("""
                INSERT INTO campo (id, negocio_id, bloque_id, nombre, tipo, opciones)
                VALUES (?, ?, ?, 'Producto', 'OPCION', '["A", "B"]')
                """, campoId, negocioId, bloqueId);
        givenFoto(loteId, 1, "segunda.jpg");
        givenFoto(loteId, 0, "primera.jpg");
        jdbcTemplate.update("UPDATE lote_subida SET bloque_destino_id = ? WHERE id = ?", bloqueId, loteId);

        LoteTrabajo lote = claim();
        List<BloqueLectura> bloques = procesamientoRepository.bloques(negocioId);

        assertThat(lote.bloqueDestinoId()).isEqualTo(bloqueId);
        assertThat(procesamientoRepository.fotos(lote)).extracting("clave")
                .containsExactly(negocioId + "/primera.jpg", negocioId + "/segunda.jpg");
        assertThat(bloques).hasSize(1);
        assertThat(bloques.getFirst().campos().getFirst().id()).isEqualTo(campoId);
        assertThat(bloques.getFirst().campos().getFirst().opciones()).containsExactly("A", "B");
        assertThat(procesamientoRepository.bloques(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Lee a qué tabla apunta cada campo de conexión")
    void bloquesShouldReadRelatedBloqueOfConexionCampo() {
        UUID proyectoId = UUID.randomUUID();
        UUID ventasId = UUID.randomUUID();
        UUID clientesId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Proyecto')",
                proyectoId, negocioId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Clientes')",
                clientesId, negocioId, proyectoId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Ventas')",
                ventasId, negocioId, proyectoId);
        jdbcTemplate.update("""
                INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones, bloque_relacionado_id)
                VALUES (?, ?, 'Cliente', 'RELACION', '[]', ?)
                """, negocioId, ventasId, clientesId);
        jdbcTemplate.update("""
                INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones) VALUES (?, ?, 'Monto', 'NUMERO', '[]')
                """, negocioId, ventasId);

        List<CampoConocido> campos = procesamientoRepository.bloques(negocioId).stream()
                .filter(bloque -> bloque.id().equals(ventasId))
                .findFirst().orElseThrow().campos();

        assertThat(campos).extracting(CampoConocido::nombre, CampoConocido::bloqueRelacionadoId)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("Cliente", clientesId),
                        org.assertj.core.groups.Tuple.tuple("Monto", null));
    }

    private UUID givenLote() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO lote_subida (id, negocio_id) VALUES (?, ?)", id, negocioId);
        return id;
    }

    private UUID givenBloqueSimulado() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Proyecto')",
                proyectoId, negocioId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Ventas')",
                bloqueId, negocioId, proyectoId);
        try (var stream = new ClassPathResource("ia/lectura-simulada.json").getInputStream()) {
            for (var campo : objectMapper.readTree(stream).path("bloque").path("campos")) {
                jdbcTemplate.update("""
                        INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones) VALUES (?, ?, ?, ?, ?::jsonb)
                        """, negocioId, bloqueId, campo.path("nombre").asString(), campo.path("tipo").asString(),
                        objectMapper.writeValueAsString(campo.path("opciones")));
            }
        }
        return bloqueId;
    }

    private void givenFoto(UUID loteId, int posicion, String clave) {
        jdbcTemplate.update("""
                INSERT INTO foto_cuaderno (negocio_id, lote_id, clave_almacenamiento, tipo_contenido,
                    tamanio_bytes, posicion) VALUES (?, ?, ?, 'image/jpeg', 3, ?)
                """, negocioId, loteId, negocioId + "/" + clave, posicion);
    }

    private LoteTrabajo claim() {
        return procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2)).orElseThrow();
    }

    private <T> List<T> concurrently(Callable<T> operacion) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch barrera = new CountDownLatch(2);
            Callable<T> sincronizada = () -> {
                barrera.countDown();
                assertThat(barrera.await(5, TimeUnit.SECONDS)).isTrue();
                return operacion.call();
            };
            var primero = executor.submit(sincronizada);
            var segundo = executor.submit(sincronizada);
            return List.of(primero.get(10, TimeUnit.SECONDS), segundo.get(10, TimeUnit.SECONDS));
        }
    }
}
