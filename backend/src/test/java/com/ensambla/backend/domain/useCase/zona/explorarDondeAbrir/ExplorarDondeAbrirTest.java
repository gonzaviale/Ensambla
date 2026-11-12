package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.DatosDondeAbrirRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExplorarDondeAbrirTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_A = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_B = ZonaId.de("c0000000-0000-4000-8000-000000000002");
    private static final ZonaId ZONA_C = ZonaId.de("c0000000-0000-4000-8000-000000000003");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);
    private static final DatosZonaDondeAbrir.Fuente OSM = new DatosZonaDondeAbrir.Fuente(
            "OSM", "OpenStreetMap", "COMUNITARIA", "https://www.openstreetmap.org/", "ODbL 1.0");
    private static final DatosZonaDondeAbrir.Fuente ENSAMBLA = new DatosZonaDondeAbrir.Fuente(
            "ENSAMBLA", "Ensambla", "ENSAMBLA", null, null);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final DatosDondeAbrirRepository datosRepository = mock(DatosDondeAbrirRepository.class);
    private final ExplorarDondeAbrir useCase = new ExplorarDondeAbrir(negocioRepository, datosRepository, CLOCK);

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocioConUbicacion());
    }

    @ParameterizedTest(name = "explica el factor {0}")
    @EnumSource(FactorDondeAbrir.class)
    @DisplayName("Normaliza, pondera y explica cada factor disponible")
    void shouldScoreAndExplainEveryFactor(FactorDondeAbrir factor) {
        DatosZonaDondeAbrir zonaA = zona(ZONA_A, "Zona A", valores(1));
        DatosZonaDondeAbrir zonaB = zona(ZONA_B, "Zona B", valores(10));
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(zonaA, zonaB));
        Map<FactorDondeAbrir, Integer> importancia = importanciaSolo(factor);
        Map<ZonaId, BigDecimal> alquileres = Map.of(ZONA_A, BigDecimal.ONE, ZONA_B, BigDecimal.TEN);

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importancia, null, alquileres));

        assertThat(resultado.ranking()).hasSize(2);
        var mejor = resultado.ranking().getFirst();
        var peor = resultado.ranking().getLast();
        String nombreEsperado = factor.menosEsMejor() ? "Zona A" : "Zona B";
        assertThat(mejor.nombre()).isEqualTo(nombreEsperado);
        assertThat(mejor.puntaje()).isEqualByComparingTo("10");
        assertThat(mejor.coincidencia()).isEqualTo("BUENA");
        assertThat(mejor.aportes()).singleElement().satisfies(aporte -> {
            assertThat(aporte.factor()).isEqualTo(factor);
            assertThat(aporte.peso()).isEqualByComparingTo("1");
            assertThat(aporte.aporte()).isEqualByComparingTo("10");
        });
        assertThat(mejor.porQue()).singleElement().asString().isNotBlank();
        assertThat(peor.coincidencia()).isEqualTo("MALA");
        assertThat(peor.riesgos()).singleElement().asString().isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(value = RubroNegocio.class, names = {"PAPELERIA", "KIOSCO"})
    @DisplayName("Usa los pesos y nombres propios de papelería y kiosco")
    void shouldSupportEveryWhereToOpenBusinessType(RubroNegocio rubro) {
        when(datosRepository.buscar(rubro)).thenReturn(List.of(
                zona(ZONA_A, "Poca", Map.of(FactorDondeAbrir.COMPETENCIA, medicion(1))),
                zona(ZONA_B, "Mucha", Map.of(FactorDondeAbrir.COMPETENCIA, medicion(10)))));

        var resultado = useCase.execute(request(rubro, PublicoDondeAbrir.OFICINAS,
                importanciaSolo(FactorDondeAbrir.COMPETENCIA), null, Map.of()));

        assertThat(resultado.ranking().getFirst().porQue().getFirst())
                .contains(rubro == RubroNegocio.PAPELERIA ? "papelerías" : "kioscos");
        assertThat(resultado.ranking().getLast().riesgos().getFirst())
                .contains(rubro == RubroNegocio.PAPELERIA ? "papelerías" : "kioscos");
    }

    @Test
    @DisplayName("Aplica todos los límites antes de puntuar")
    void shouldDiscardBeforeScoring() {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = new EnumMap<>(FactorDondeAbrir.class);
        metricas.put(FactorDondeAbrir.COMPETENCIA, medicion(10));
        metricas.put(FactorDondeAbrir.ESCUELAS, medicion(0));
        metricas.put(FactorDondeAbrir.PARQUES, medicion(0));
        metricas.put(FactorDondeAbrir.TRANSPORTE, medicion(0));
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(zona(ZONA_A, "Lejana", metricas)));
        var limites = new ExplorarDondeAbrirRequest.Limites(
                BigDecimal.ONE, 1, 2, 2, 2, BigDecimal.valueOf(500));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.ESTUDIANTES,
                Map.of(), limites, Map.of(ZONA_A, BigDecimal.valueOf(1_000))));

        assertThat(resultado.ranking()).isEmpty();
        assertThat(resultado.descartadas()).singleElement().satisfies(descartada ->
                assertThat(descartada.motivos()).hasSize(6));
    }

    @Test
    @DisplayName("Informa cuando falta ubicación y tolera una zona sin métricas ni factores elegidos")
    void shouldDegradeGracefullyWhenDataIsMissing() {
        Negocio sinUbicacion = Negocio.builder().nombre("Sin ubicación").rubro(RubroNegocio.ALMACEN).build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(sinUbicacion);
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(zona(ZONA_A, "Sin datos", Map.of())));
        Map<FactorDondeAbrir, Integer> ningunaImporta = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            ningunaImporta.put(factor, 0);
        }

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                ningunaImporta, new ExplorarDondeAbrirRequest.Limites(BigDecimal.ONE, null, 0, 0, 0, null),
                Map.of()));

        assertThat(resultado.ranking()).singleElement().satisfies(zona -> {
            assertThat(zona.distanciaKm()).isNull();
            assertThat(zona.puntaje()).isEqualByComparingTo("0");
            assertThat(zona.coincidencia()).isEqualTo("MALA");
            assertThat(zona.aportes()).isEmpty();
            assertThat(zona.porQue()).containsExactly("Ningún factor se destaca: es una zona pareja");
        });
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ubicación"));
    }

    @Test
    @DisplayName("Penaliza una zona a menos de un kilómetro y avisa si una fuente está desactualizada")
    void shouldPenalizeCannibalizationAndWarnAboutStaleData() {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = Map.of(
                FactorDondeAbrir.ACTIVIDAD_ACCESO,
                new DatosZonaDondeAbrir.Medicion(BigDecimal.TEN, "indice", OSM,
                        LocalDate.parse("2026-01-01")));
        DatosZonaDondeAbrir cercana = new DatosZonaDondeAbrir(ZONA_A, "Cercana", "CABA", "CABA", "CABA",
                new BigDecimal("-34.6001"), new BigDecimal("-58.4001"), metricas, List.of(punto()));
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(cercana));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of()));

        assertThat(resultado.ranking()).singleElement().satisfies(zona -> {
            assertThat(zona.puntaje()).isEqualByComparingTo("3.8");
            assertThat(zona.coincidencia()).isEqualTo("MALA");
            assertThat(zona.riesgos().getFirst()).contains("quitarse clientes");
            assertThat(zona.puntos()).hasSize(1);
            assertThat(zona.jurisdiccion()).isEqualTo("CABA");
        });
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("90 días"));
    }

    @Test
    @DisplayName("Una zona con un único valor comparable obtiene un subpuntaje intermedio")
    void shouldUseMiddleScoreWhenRangeHasOneValue() {
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Única", Map.of(FactorDondeAbrir.ESCUELAS, medicion(3)))));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.ESTUDIANTES,
                importanciaSolo(FactorDondeAbrir.ESCUELAS), null, Map.of()));

        assertThat(resultado.ranking().getFirst().puntaje()).isEqualByComparingTo("5");
        assertThat(resultado.ranking().getFirst().coincidencia()).isEqualTo("INTERMEDIA");
        assertThat(resultado.ranking().getFirst().riesgos())
                .containsExactly("Son datos estimados: confirmalos caminando la zona antes de decidir");
    }

    @Test
    @DisplayName("Desempata por distancia cuando dos zonas tienen el mismo puntaje")
    void shouldOrderScoreTiesByDistance() {
        var lejos = zonaEn(ZONA_B, "Lejos", "-35.1", "-59.1",
                Map.of(FactorDondeAbrir.ESCUELAS, medicion(3)));
        var cerca = zonaEn(ZONA_A, "Cerca", "-34.61", "-58.41",
                Map.of(FactorDondeAbrir.ESCUELAS, medicion(3)));
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(lejos, cerca));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.ESTUDIANTES,
                importanciaSolo(FactorDondeAbrir.ESCUELAS), null, Map.of()));

        assertThat(resultado.ranking()).extracting(ExplorarDondeAbrirResponse.ZonaEvaluada::nombre)
                .containsExactly("Cerca", "Lejos");
    }

    @Test
    @DisplayName("Descarta y ordena zonas sin distancia cuando faltan las métricas exigidas")
    void shouldDiscardMissingMetricsWithoutBusinessLocation() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().nombre("Sin ubicación").rubro(RubroNegocio.ALMACEN).build());
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Sin datos A", Map.of()), zona(ZONA_B, "Sin datos B", Map.of())));
        var limites = new ExplorarDondeAbrirRequest.Limites(null, 2, 1, null, null, null);

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of()));

        assertThat(resultado.ranking()).isEmpty();
        assertThat(resultado.descartadas()).hasSize(2).allSatisfy(descartada -> {
            assertThat(descartada.distanciaKm()).isNull();
            assertThat(descartada.motivos()).hasSize(2)
                    .allMatch(motivo -> motivo.startsWith("No hay datos de"));
        });
    }

    @Test
    @DisplayName("Conserva el orden si hay empate y el negocio todavía no tiene ubicación")
    void shouldKeepOrderForScoreTiesWithoutBusinessLocation() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().nombre("Sin ubicación").rubro(RubroNegocio.ALMACEN).build());
        var metricas = Map.of(FactorDondeAbrir.ESCUELAS, medicion(3));
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Primera", metricas), zona(ZONA_B, "Segunda", metricas)));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.ESTUDIANTES,
                importanciaSolo(FactorDondeAbrir.ESCUELAS), null, Map.of()));

        assertThat(resultado.ranking()).extracting(ExplorarDondeAbrirResponse.ZonaEvaluada::nombre)
                .containsExactly("Primera", "Segunda");
    }

    @Test
    @DisplayName("Ordena las zonas descartadas desde la más cercana")
    void shouldOrderDiscardedZonesByDistance() {
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zonaEn(ZONA_B, "Lejana", "-35.1", "-59.1", Map.of()),
                zonaEn(ZONA_A, "Cercana", "-34.61", "-58.41", Map.of())));
        var limites = new ExplorarDondeAbrirRequest.Limites(null, 2, null, null, null, null);

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of()));

        assertThat(resultado.descartadas()).extracting(ExplorarDondeAbrirResponse.ZonaDescartada::nombre)
                .containsExactly("Cercana", "Lejana");
    }

    @Test
    @DisplayName("Conserva zonas que cumplen los límites, tengan o no una cotización de alquiler")
    void shouldKeepZonesThatMeetEveryLimit() {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = Map.of(
                FactorDondeAbrir.COMPETENCIA, medicion(2), FactorDondeAbrir.ESCUELAS, medicion(2));
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Cotizada", metricas), zona(ZONA_B, "Sin cotizar", metricas)));
        var limites = new ExplorarDondeAbrirRequest.Limites(
                BigDecimal.valueOf(500), 2, 2, null, null, BigDecimal.valueOf(500));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of(ZONA_A, BigDecimal.valueOf(500))));

        assertThat(resultado.ranking()).hasSize(2);
        assertThat(resultado.descartadas()).isEmpty();
    }

    @Test
    @DisplayName("Reparte el peso por igual si la persona desactiva todos los factores disponibles")
    void shouldUseEqualWeightsWhenEveryConfiguredWeightIsZero() {
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Única", Map.of(FactorDondeAbrir.ESCUELAS, medicion(3)))));
        Map<FactorDondeAbrir, Integer> ningunaImporta = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            ningunaImporta.put(factor, 0);
        }

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                ningunaImporta, null, Map.of()));

        assertThat(resultado.ranking().getFirst().aportes()).singleElement().satisfies(aporte ->
                assertThat(aporte.peso()).isEqualByComparingTo("1"));
    }

    @Test
    @DisplayName("Tolera mapas nulos, una coordenada incompleta y una fecha de medición desconocida")
    void shouldTolerateNullMapsPartialCoordinatesAndUnknownDate() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(Negocio.builder()
                .nombre("Coordenadas incompletas").rubro(RubroNegocio.ALMACEN)
                .latitud(new BigDecimal("-34.6")).build());
        var sinFecha = new DatosZonaDondeAbrir.Medicion(BigDecimal.ONE, "lugares_500m", OSM, null);
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Sin fecha", Map.of(FactorDondeAbrir.ESCUELAS, sinFecha))));
        var pedido = new ExplorarDondeAbrirRequest(
                USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, null, null, null);

        var resultado = useCase.execute(pedido);

        assertThat(resultado.ranking().getFirst().distanciaKm()).isNull();
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ubicación"))
                .noneMatch(aviso -> aviso.contains("90 días"));
    }

    @Test
    @DisplayName("Ordena varios riesgos y omite los factores con peso insignificante")
    void shouldOrderRelevantRisks() {
        when(datosRepository.buscar(RubroNegocio.ALMACEN)).thenReturn(List.of(
                zona(ZONA_A, "Baja", valores(1)), zona(ZONA_C, "Alta", valores(10))));

        var resultado = useCase.execute(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of()));

        assertThat(resultado.ranking()).allSatisfy(zona -> assertThat(zona.riesgos()).hasSizeLessThanOrEqualTo(2));
        assertThat(resultado.ranking()).anySatisfy(zona -> assertThat(zona.riesgos()).hasSize(2));
    }

    @ParameterizedTest(name = "rechaza {0}")
    @MethodSource("requestsInvalidos")
    @DisplayName("Rechaza filtros incompletos o inválidos")
    void shouldRejectInvalidRequests(String caso, ExplorarDondeAbrirRequest request) {
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static Stream<Object[]> requestsInvalidos() {
        var base = new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of());
        return Stream.of(
                new Object[]{"pedido nulo", null},
                new Object[]{"usuario nulo", new ExplorarDondeAbrirRequest(null, base.rubro(), base.publico(),
                        base.importancia(), null, Map.of())},
                new Object[]{"rubro nulo", new ExplorarDondeAbrirRequest(USUARIO_ID, null, base.publico(),
                        base.importancia(), null, Map.of())},
                new Object[]{"público nulo", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(), null,
                        base.importancia(), null, Map.of())},
                new Object[]{"rubro no disponible", new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.OTRO,
                        base.publico(), Map.of(), null, Map.of())},
                new Object[]{"importancia menor", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(),
                        base.publico(), Map.of(FactorDondeAbrir.ESCUELAS, -1), null, Map.of())},
                new Object[]{"importancia mayor", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(),
                        base.publico(), Map.of(FactorDondeAbrir.ESCUELAS, 3), null, Map.of())},
                new Object[]{"importancia nula", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(),
                        base.publico(), mapaConValorNulo(), null, Map.of())},
                new Object[]{"zona de alquiler nula", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(),
                        base.publico(), Map.of(), null, mapaAlquilerConZonaNula())},
                new Object[]{"alquiler nulo", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(),
                        base.publico(), Map.of(), null, mapaAlquilerConValorNulo())},
                new Object[]{"alquiler cero", new ExplorarDondeAbrirRequest(USUARIO_ID, base.rubro(),
                        base.publico(), Map.of(), null, Map.of(ZONA_A, BigDecimal.ZERO))},
                new Object[]{"distancia negativa", conLimites(new BigDecimal("-1"), 0, 0, 0, 0, BigDecimal.ZERO)},
                new Object[]{"competencia negativa", conLimites(BigDecimal.ZERO, -1, 0, 0, 0, BigDecimal.ZERO)},
                new Object[]{"escuelas negativas", conLimites(BigDecimal.ZERO, 0, -1, 0, 0, BigDecimal.ZERO)},
                new Object[]{"parques negativos", conLimites(BigDecimal.ZERO, 0, 0, -1, 0, BigDecimal.ZERO)},
                new Object[]{"transporte negativo", conLimites(BigDecimal.ZERO, 0, 0, 0, -1, BigDecimal.ZERO)},
                new Object[]{"alquiler máximo negativo", conLimites(BigDecimal.ZERO, 0, 0, 0, 0,
                        new BigDecimal("-1"))});
    }

    private static ExplorarDondeAbrirRequest conLimites(BigDecimal distancia, Integer competencia,
                                                         Integer escuelas, Integer parques, Integer transporte,
                                                         BigDecimal alquiler) {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, Map.of(),
                new ExplorarDondeAbrirRequest.Limites(distancia, competencia, escuelas, parques, transporte, alquiler),
                Map.of());
    }

    private static Map<FactorDondeAbrir, Integer> mapaConValorNulo() {
        Map<FactorDondeAbrir, Integer> mapa = new EnumMap<>(FactorDondeAbrir.class);
        mapa.put(FactorDondeAbrir.ESCUELAS, null);
        return mapa;
    }

    private static Map<ZonaId, BigDecimal> mapaAlquilerConZonaNula() {
        Map<ZonaId, BigDecimal> mapa = new java.util.HashMap<>();
        mapa.put(null, BigDecimal.ONE);
        return mapa;
    }

    private static Map<ZonaId, BigDecimal> mapaAlquilerConValorNulo() {
        Map<ZonaId, BigDecimal> mapa = new java.util.HashMap<>();
        mapa.put(ZONA_A, null);
        return mapa;
    }

    private static ExplorarDondeAbrirRequest request(RubroNegocio rubro, PublicoDondeAbrir publico,
                                                      Map<FactorDondeAbrir, Integer> importancia,
                                                      ExplorarDondeAbrirRequest.Limites limites,
                                                      Map<ZonaId, BigDecimal> alquileres) {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, rubro, publico, importancia, limites, alquileres);
    }

    private static Map<FactorDondeAbrir, Integer> importanciaSolo(FactorDondeAbrir elegido) {
        Map<FactorDondeAbrir, Integer> importancia = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            importancia.put(factor, factor == elegido ? 2 : 0);
        }
        return importancia;
    }

    private static Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> valores(int valor) {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            if (factor != FactorDondeAbrir.ALQUILER) {
                DatosZonaDondeAbrir.Fuente fuente = factor == FactorDondeAbrir.DEMANDA_ENSAMBLA ? ENSAMBLA : OSM;
                metricas.put(factor, new DatosZonaDondeAbrir.Medicion(BigDecimal.valueOf(valor), "unidad", fuente,
                        LocalDate.parse("2026-11-10")));
            }
        }
        return metricas;
    }

    private static DatosZonaDondeAbrir.Medicion medicion(int valor) {
        return new DatosZonaDondeAbrir.Medicion(BigDecimal.valueOf(valor), "lugares_500m", OSM,
                LocalDate.parse("2026-11-10"));
    }

    private static DatosZonaDondeAbrir zona(ZonaId id, String nombre,
                                             Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas) {
        return zonaEn(id, nombre, "-34.65", "-58.55", metricas);
    }

    private static DatosZonaDondeAbrir zonaEn(ZonaId id, String nombre, String latitud, String longitud,
                                                Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas) {
        return new DatosZonaDondeAbrir(id, nombre, "Localidad", "La Matanza", "Buenos Aires",
                new BigDecimal(latitud), new BigDecimal(longitud), metricas, List.of());
    }

    private static DatosZonaDondeAbrir.Punto punto() {
        return new DatosZonaDondeAbrir.Punto("node:1", "ESCUELA", null, "Escuela", new BigDecimal("-34.6"),
                new BigDecimal("-58.4"), OSM, LocalDate.parse("2026-01-01"));
    }

    private static Negocio negocioConUbicacion() {
        return Negocio.builder().nombre("Almacén actual").rubro(RubroNegocio.ALMACEN).direccion("Arieta 1")
                .latitud(new BigDecimal("-34.6")).longitud(new BigDecimal("-58.4"))
                .zonaId(ZonaId.de(UUID.fromString("c0000000-0000-4000-8000-000000000099"))).build();
    }
}
