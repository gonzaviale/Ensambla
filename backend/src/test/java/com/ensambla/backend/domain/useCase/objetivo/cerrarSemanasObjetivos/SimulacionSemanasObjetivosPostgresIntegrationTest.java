package com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanas;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ReajusteItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.SemanaItem;
import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPaso;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPasoRequest;
import com.ensambla.backend.support.AlmacenSimulado;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-184: simula 8 semanas de un almacén con PostgreSQL en cuatro escenarios (avanza, se estanca, lo logra pronto,
 * retrocede). Un reloj de prueba avanza día por día: cada mediodía corre el monitoreo y cada domingo a las 23:45, el
 * cierre (dos veces, como si el servidor se reiniciara). Al terminar deja en {@code target/simulacion-objetivos.md} lo
 * que pasó cada semana en cada escenario, para el informe.
 */
@Transactional
class SimulacionSemanasObjetivosPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final int SEMANAS = 8;
    private static final int DIAS_DE_HISTORIA = 35;
    private static final LocalTime MEDIODIA = LocalTime.NOON;
    private static final LocalTime CIERRE = LocalTime.of(23, 45);
    private static final ZoneOffset ARGENTINA = ZoneOffset.ofHours(-3);
    private static final Map<Escenario, List<SemanaItem>> RESULTADOS = new EnumMap<>(Escenario.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SistemaRepository sistemaRepository;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private ObjetivoRepository objetivoRepository;

    @Autowired
    private MiniObjetivoRepository miniObjetivoRepository;

    @Autowired
    private ProgresoObjetivoRepository progresoObjetivoRepository;

    @Autowired
    private DecisionObjetivoRepository decisionObjetivoRepository;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private ResumenSemanalRepository resumenSemanalRepository;

    @Autowired
    private PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    @Autowired
    private TransaccionPort transaccionPort;

    @Autowired
    private ListarSemanas listarSemanas;

    @PersistenceContext
    private EntityManager entityManager;

    private final RelojDePrueba reloj = new RelojDePrueba();
    private LocalDate inicio;
    private MonitorearObjetivos monitorear;
    private CerrarSemanasObjetivos cerrar;
    private ObtenerProgreso obtenerProgreso;
    private RegistrarDecisionPaso registrarDecision;

    @BeforeEach
    void setUp() {
        inicio = SemanaObjetivo.lunesDe(LocalDate.now(ZonaNegocio.ZONA)).minusWeeks(SEMANAS + 1L);
        monitorear = new MonitorearObjetivos(objetivoRepository, new MonitoreoObjetivosService(objetivoRepository,
                miniObjetivoRepository, progresoObjetivoRepository, propuestaMiniObjetivosService), transaccionPort,
                reloj, 50);
        cerrar = new CerrarSemanasObjetivos(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository,
                avisoRepository, resumenSemanalRepository, propuestaMiniObjetivosService, transaccionPort, reloj,
                SEMANAS, 50);
        obtenerProgreso = new ObtenerProgreso(negocioRepository, objetivoRepository, miniObjetivoRepository,
                progresoObjetivoRepository, propuestaMiniObjetivosService, resumenSemanalRepository, reloj);
        registrarDecision = new RegistrarDecisionPaso(negocioRepository, objetivoRepository, miniObjetivoRepository,
                decisionObjetivoRepository, transaccionPort, reloj);
    }

    @AfterAll
    static void escribirInforme() throws IOException {
        StringBuilder informe = new StringBuilder("# Simulación de 8 semanas por escenario (T-184)\n");
        RESULTADOS.forEach((escenario, semanas) -> {
            informe.append("\n## ").append(escenario.titulo).append("\n\n");
            semanas.stream().sorted(Comparator.comparing(SemanaItem::semana)).forEach(semana -> {
                informe.append("- **Semana del ").append(semana.semana()).append("**: ")
                        .append(semana.resultado().toPlainString()).append(" (")
                        .append(semana.avance().movePointRight(2).stripTrailingZeros().toPlainString())
                        .append(" % de la meta). ").append(semana.queFunciono()).append('\n');
                semana.reajustes().forEach(reajuste -> informe.append("  - ").append(reajuste.tipo()).append(": ")
                        .append(reajuste.motivo()).append('\n'));
            });
        });
        Path destino = Path.of("target", "simulacion-objetivos.md");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, informe.toString());
    }

    @Test
    @DisplayName("Si el negocio avanza, sube la meta de un mini-objetivo logrado")
    void simularShouldRaiseTheMetaWhenTheBusinessAdvances() {
        Simulacion simulacion = givenSimulacion(Escenario.AVANZA);

        List<SemanaItem> semanas = simular(simulacion);

        assertThat(tiposDe(semanas)).contains(TipoReajuste.SUBIR_META);
        assertThat(semanas.getFirst().resultado()).isGreaterThan(semanas.getLast().resultado());
    }

    @Test
    @DisplayName("Si el negocio se estanca, cambia lo que no avanzó en dos cierres y explica por qué")
    void simularShouldReplaceWhatDoesNotAdvanceWhenTheBusinessStalls() {
        Simulacion simulacion = givenSimulacion(Escenario.SE_ESTANCA);

        List<SemanaItem> semanas = simular(simulacion);

        assertThat(reajustesDe(semanas))
                .anySatisfy(reajuste -> {
                    assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
                    assertThat(reajuste.motivo()).startsWith("Hace 2 semanas que «").contains("no avanza");
                });
        assertThat(tiposDe(semanas)).doesNotContain(TipoReajuste.SUBIR_META);
    }

    @Test
    @DisplayName("Si lo logra pronto, marca el objetivo como alcanzado y ya no reajusta")
    void simularShouldStopAdjustingWhenTheObjetivoIsReachedEarly() {
        Simulacion simulacion = givenSimulacion(Escenario.LO_LOGRA_PRONTO);

        List<SemanaItem> semanas = simular(simulacion);

        assertThat(alcanzado(simulacion)).isTrue();
        assertThat(semanas.getFirst().avance()).isGreaterThanOrEqualTo(BigDecimal.ONE);
        assertThat(semanas.stream().filter(semana -> semana.avance().compareTo(BigDecimal.ONE) >= 0))
                .allSatisfy(semana -> assertThat(semana.reajustes()).isEmpty());
    }

    @Test
    @DisplayName("Si el negocio retrocede, lo cuenta en el resumen y cambia lo que no avanza")
    void simularShouldReportTheDropAndReplaceWhenTheBusinessGoesBack() {
        Simulacion simulacion = givenSimulacion(Escenario.RETROCEDE);

        List<SemanaItem> semanas = simular(simulacion);

        assertThat(semanas.getFirst().queFunciono()).startsWith("Esta semana tu resultado bajó ");
        assertThat(semanas.getFirst().resultado()).isLessThan(semanas.getLast().resultado());
        assertThat(tiposDe(semanas)).contains(TipoReajuste.REEMPLAZAR);
    }

    @Test
    @DisplayName("En ningún escenario el monitoreo ni el cierre crean avisos")
    void simularShouldNotCreateNoticesInAnyScenario() {
        int avisosAntes = contar("aviso");

        for (Escenario escenario : Escenario.values()) {
            simular(givenSimulacion(escenario));
        }

        assertThat(contar("aviso")).isEqualTo(avisosAntes);
    }

    @Test
    @DisplayName("Cada escenario tiene un solo resumen por semana aunque el cierre corra dos veces")
    void simularShouldKeepOneSummaryPerWeekInEveryScenario() {
        for (Escenario escenario : Escenario.values()) {
            Simulacion simulacion = givenSimulacion(escenario);

            List<SemanaItem> semanas = simular(simulacion);

            assertThat(semanas).extracting(SemanaItem::semana)
                    .doesNotHaveDuplicates()
                    .hasSize(SEMANAS)
                    .containsExactlyElementsOf(lunesDeLaSimulacion().reversed());
        }
    }

    @Test
    @DisplayName("En cada escenario el gráfico muestra, para cada semana cerrada, el mismo resultado que el resumen")
    void simularShouldDrawTheSameWeeklyResultAsTheSummary() {
        for (Escenario escenario : Escenario.values()) {
            Simulacion simulacion = givenSimulacion(escenario);

            List<SemanaItem> semanas = simular(simulacion);

            Map<LocalDate, BigDecimal> grafico = graficoDe(simulacion);
            assertThat(semanas).allSatisfy(semana -> assertThat(grafico.get(semana.semana()))
                    .as("%s, semana del %s", escenario, semana.semana())
                    .isEqualByComparingTo(semana.resultado()));
        }
    }

    // --- arrange ---
    private Simulacion givenSimulacion(Escenario escenario) {
        UUID usuarioId = UUID.randomUUID();
        UUID negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Simulación')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, ?, ?)",
                negocioId, escenario.titulo, usuarioId);
        AlmacenSimulado.guardar(sistemaRepository, negocioId, inicio.minusDays(DIAS_DE_HISTORIA),
                inicio.plusWeeks(SEMANAS).minusDays(1), dia -> escenario.factor(semanaDe(dia)));
        UUID objetivoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta, creado_en) VALUES (?, ?, ?, ?, ?)",
                objetivoId, negocioId, "Quiero ganar más", escenario.meta, instante(inicio, LocalTime.of(9, 0)));
        return new Simulacion(escenario, UsuarioId.de(usuarioId), NegocioId.de(negocioId), ObjetivoId.de(objetivoId));
    }

    // --- act ---
    /** Corre las 8 semanas día por día y devuelve los resúmenes, del más nuevo al más viejo. */
    private List<SemanaItem> simular(Simulacion simulacion) {
        for (LocalDate dia = inicio; dia.isBefore(inicio.plusWeeks(SEMANAS)); dia = dia.plusDays(1)) {
            reloj.en(instante(dia, MEDIODIA));
            monitorear.execute();
            decidirSiToca(simulacion, dia);
            if (dia.getDayOfWeek() == inicio.plusDays(6).getDayOfWeek()) {
                reloj.en(instante(dia, CIERRE));
                cerrar.execute();
                cerrar.execute();
            }
        }
        entityManager.flush();
        List<SemanaItem> semanas = listarSemanas.execute(
                new ListarSemanasRequest(simulacion.usuarioId(), simulacion.objetivoId())).semanas();
        RESULTADOS.putIfAbsent(simulacion.escenario(), semanas);
        return semanas;
    }

    /** El martes de la semana 2 acepta el paso del primer mini-objetivo; el de la semana 4 ignora el del segundo. */
    private void decidirSiToca(Simulacion simulacion, LocalDate dia) {
        if (dia.equals(inicio.plusWeeks(1).plusDays(1))) {
            decidir(simulacion, 0, DecisionPaso.ACEPTADO);
        } else if (dia.equals(inicio.plusWeeks(3).plusDays(1))) {
            decidir(simulacion, 1, DecisionPaso.IGNORADO);
        }
    }

    private void decidir(Simulacion simulacion, int orden, DecisionPaso decision) {
        List<MiniObjetivo> enCurso = miniObjetivoRepository
                .findAllByObjetivoIdAndNegocioId(simulacion.objetivoId(), simulacion.negocioId()).stream()
                .filter(MiniObjetivo::estaEnCurso)
                .sorted(Comparator.comparingInt(MiniObjetivo::getPosicion))
                .toList();
        if (enCurso.size() > orden) {
            registrarDecision.execute(new RegistrarDecisionPasoRequest(
                    simulacion.usuarioId(), simulacion.objetivoId(), enCurso.get(orden).getId(), decision));
        }
    }

    // --- assert ---
    private static List<TipoReajuste> tiposDe(List<SemanaItem> semanas) {
        return reajustesDe(semanas).stream().map(ReajusteItem::tipo).toList();
    }

    private static List<ReajusteItem> reajustesDe(List<SemanaItem> semanas) {
        return semanas.stream().flatMap(semana -> semana.reajustes().stream()).toList();
    }

    private boolean alcanzado(Simulacion simulacion) {
        return objetivoRepository.obtener(simulacion.negocioId(), simulacion.objetivoId()).estaAlcanzado();
    }

    private Map<LocalDate, BigDecimal> graficoDe(Simulacion simulacion) {
        return obtenerProgreso.execute(new ObtenerProgresoRequest(simulacion.usuarioId(), simulacion.objetivoId()))
                .semanas().stream()
                .collect(Collectors.toMap(PuntoSemanal::semana, PuntoSemanal::valor));
    }

    private int contar(String tabla) {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    // --- helpers ---
    /** La semana de la simulación en la que cae el día: -1 antes de empezar, de 0 a 7 durante. */
    private int semanaDe(LocalDate dia) {
        return dia.isBefore(inicio) ? -1 : (int) (ChronoUnit.DAYS.between(inicio, dia) / 7);
    }

    private List<LocalDate> lunesDeLaSimulacion() {
        return IntStream.range(0, SEMANAS).mapToObj(inicio::plusWeeks).toList();
    }

    private static OffsetDateTime instante(LocalDate dia, LocalTime hora) {
        return OffsetDateTime.of(dia, hora, ARGENTINA);
    }

    private record Simulacion(Escenario escenario, UsuarioId usuarioId, NegocioId negocioId, ObjetivoId objetivoId) {
    }

    /** Cómo se mueven las ventas cada semana y la meta mensual de cada escenario. */
    private enum Escenario {
        AVANZA("Avanza: las ventas suben 8 % por semana", 1_300_000, semana -> 1 + 0.08 * (semana + 1)),
        SE_ESTANCA("Se estanca: vende lo mismo todas las semanas", 1_300_000, semana -> 1),
        LO_LOGRA_PRONTO("Lo logra pronto: las ventas saltan 35 % la primera semana", 760_000, semana -> 1.35),
        RETROCEDE("Retrocede: las ventas bajan 7 % por semana", 1_300_000,
                semana -> Math.max(0.3, 1 - 0.07 * (semana + 1)));

        private final String titulo;
        private final BigDecimal meta;
        private final IntToDoubleFunction factorPorSemana;

        Escenario(String titulo, long meta, IntToDoubleFunction factorPorSemana) {
            this.titulo = titulo;
            this.meta = BigDecimal.valueOf(meta);
            this.factorPorSemana = factorPorSemana;
        }

        double factor(int semana) {
            return semana < 0 ? 1 : factorPorSemana.applyAsDouble(semana);
        }
    }

    /** Un reloj que la simulación mueve a mano. */
    private static final class RelojDePrueba extends Clock {

        private Instant ahora = Instant.now();

        void en(OffsetDateTime instante) {
            ahora = instante.toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
