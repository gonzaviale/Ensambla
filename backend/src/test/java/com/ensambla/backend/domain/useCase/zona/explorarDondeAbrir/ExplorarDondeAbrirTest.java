package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.EntornoDelPuntoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.DatosDondeAbrirRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExplorarDondeAbrirTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_A = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_B = ZonaId.de("c0000000-0000-4000-8000-000000000002");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);
    private static final DatosZonaDondeAbrir.Fuente OSM = new DatosZonaDondeAbrir.Fuente(
            "OSM", "OpenStreetMap", "COMUNITARIA", "https://www.openstreetmap.org/", "ODbL 1.0");
    private static final DatosZonaDondeAbrir.Fuente ENSAMBLA = new DatosZonaDondeAbrir.Fuente(
            "ENSAMBLA", "Ensambla", "ENSAMBLA", null, null);
    private static final Map<ZonaId, BigDecimal> ALQUILERES = Map.of(ZONA_A, BigDecimal.ONE, ZONA_B, BigDecimal.TEN);
    private static final Negocio SIN_UBICACION =
            Negocio.builder().nombre("Sin ubicación").rubro(RubroNegocio.ALMACEN).build();

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final DatosDondeAbrirRepository datosRepository = mock(DatosDondeAbrirRepository.class);
    private final EntornoDelPuntoPort entornoPort = mock(EntornoDelPuntoPort.class);
    private final ExplorarDondeAbrir useCase =
            new ExplorarDondeAbrir(negocioRepository, datosRepository, entornoPort, CLOCK);

    private ExplorarDondeAbrirResponse resultado;
    private Throwable error;

    @BeforeEach
    void setUp() {
        givenNegocio(negocioConUbicacion());
    }

    @ParameterizedTest(name = "explica el factor {0}")
    @EnumSource(value = FactorDondeAbrir.class, names = {"COMPETENCIA", "ALQUILER"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Cuando más es mejor, la zona con el valor más alto encabeza el ranking")
    void executeShouldRankHighestValueFirstWhenMoreIsBetter(FactorDondeAbrir factor) {
        givenDosZonasQueMidenUnoYDiez();

        whenSeExploraSoloPorConAlquileres(factor);

        thenLaMejorZonaEs("Zona B", factor);
    }

    @ParameterizedTest(name = "explica el factor {0}")
    @EnumSource(value = FactorDondeAbrir.class, names = {"COMPETENCIA", "ALQUILER"})
    @DisplayName("Cuando menos es mejor, la zona con el valor más bajo encabeza el ranking")
    void executeShouldRankLowestValueFirstWhenLessIsBetter(FactorDondeAbrir factor) {
        givenDosZonasQueMidenUnoYDiez();

        whenSeExploraSoloPorConAlquileres(factor);

        thenLaMejorZonaEs("Zona A", factor);
    }

    @ParameterizedTest
    @CsvSource({"PAPELERIA,papelerías", "KIOSCO,kioscos"})
    @DisplayName("Usa los pesos y nombres propios de papelería y kiosco")
    void executeShouldNameTheTradeWhenItIsStationeryOrKiosk(RubroNegocio rubro, String plural) {
        givenCompetenciaBajaYAltaPara(rubro);

        whenSeExploraLaCompetenciaPara(rubro);

        thenLasFrasesNombranA(plural);
    }

    @Test
    @DisplayName("Aplica todos los límites antes de puntuar")
    void executeShouldDiscardZoneBeforeScoringWhenEveryLimitIsExceeded() {
        givenZonaQueExcedeTodosLosLimites();

        whenSeExploraConTodosLosLimites();

        thenSeDescartaLaZonaPorSeisMotivos();
    }

    @Test
    @DisplayName("Informa cuando falta ubicación y tolera una zona sin métricas ni factores elegidos")
    void executeShouldWarnAboutLocationWhenBusinessHasNoLocation() {
        givenNegocio(SIN_UBICACION);
        givenZonas(RubroNegocio.ALMACEN, List.of(zona(ZONA_A, "Sin datos", Map.of())));

        whenSeExploraSinNingunaImportancia();

        thenLaZonaQuedaNeutraYSeAvisaQueFaltaLaUbicacion();
    }

    @Test
    @DisplayName("Penaliza una zona a menos de un kilómetro y avisa si una fuente está desactualizada")
    void executeShouldPenalizeCannibalizationAndWarnWhenSourceIsStale() {
        givenZonaCercanaConUnaFuenteVieja();

        whenSeExploraConLosValoresPorDefecto();

        thenSePenalizaLaZonaCercanaYSeAvisaDeLaFuenteVieja();
    }

    @Test
    @DisplayName("Una zona con un único valor comparable obtiene un subpuntaje intermedio")
    void executeShouldGiveMiddleScoreWhenRangeHasOneValue() {
        givenUnaZonaConEscuelasMedidas(3);

        whenSeExploraSoloPor(FactorDondeAbrir.ESCUELAS);

        thenLaZonaQuedaIntermediaConUnRiesgoGenerico();
    }

    @Test
    @DisplayName("Desempata por distancia cuando dos zonas tienen el mismo puntaje")
    void executeShouldOrderScoreTiesByDistanceWhenZonesScoreTheSame() {
        givenDosZonasConLasMismasEscuelasUnaLejosYUnaCerca();

        whenSeExploraSoloPor(FactorDondeAbrir.ESCUELAS);

        thenElRankingEs("Cerca", "Lejos");
    }

    @Test
    @DisplayName("Descarta y ordena zonas sin distancia cuando faltan las métricas exigidas")
    void executeShouldDiscardZonesWhenRequiredMetricsAreMissingAndThereIsNoLocation() {
        givenNegocio(SIN_UBICACION);
        givenDosZonasSinMetricas();

        whenSeExploraConLimitesDeCompetenciaYEscuelas();

        thenSeDescartanLasDosZonasPorFaltaDeDatos();
    }

    @Test
    @DisplayName("Conserva el orden si hay empate y el negocio todavía no tiene ubicación")
    void executeShouldKeepOrderWhenScoresTieAndBusinessHasNoLocation() {
        givenNegocio(SIN_UBICACION);
        givenDosZonasIgualesLlamadasPrimeraYSegunda();

        whenSeExploraSoloPor(FactorDondeAbrir.ESCUELAS);

        thenElRankingEs("Primera", "Segunda");
    }

    @Test
    @DisplayName("Ordena las zonas descartadas desde la más cercana")
    void executeShouldOrderDiscardedZonesFromTheNearestWhenSeveralAreDiscarded() {
        givenDosZonasSinMetricasUnaLejanaYUnaCercana();

        whenSeExploraConUnLimiteDeCompetencia();

        thenLasDescartadasSon("Cercana", "Lejana");
    }

    @Test
    @DisplayName("Conserva zonas que cumplen los límites, tengan o no una cotización de alquiler")
    void executeShouldKeepZonesWhenTheyMeetEveryLimit() {
        givenDosZonasQueCumplenLosLimites();

        whenSeExploraConTodosLosLimitesYUnaSolaCotizacion();

        thenNingunaZonaSeDescarta();
    }

    @Test
    @DisplayName("Reparte el peso por igual si la persona desactiva todos los factores disponibles")
    void executeShouldSplitWeightEquallyWhenEveryFactorIsDeactivated() {
        givenUnaZonaConEscuelasMedidas(3);

        whenSeExploraSinNingunaImportanciaNiLimites();

        thenElUnicoFactorPesaTodo();
    }

    @Test
    @DisplayName("Tolera mapas nulos, una coordenada incompleta y una fecha de medición desconocida")
    void executeShouldTolerateNullMapsPartialCoordinatesAndUnknownDate() {
        givenNegocioConCoordenadasIncompletas();
        givenUnaZonaSinFechaDeMedicion();

        whenSeExploraConMapasNulos();

        thenNoHayDistanciaNiAvisoDeFuenteVieja();
    }

    @Test
    @DisplayName("Ordena varios riesgos y omite los factores con peso insignificante")
    void executeShouldKeepAtMostTwoRisksWhenSeveralFactorsAreWeak() {
        givenDosZonasQueMidenUnoYDiez();

        whenSeExploraConLosValoresPorDefecto();

        thenCadaZonaTieneComoMuchoDosRiesgosYAlgunaTieneDos();
    }

    @Test
    @DisplayName("Mide en vivo el punto elegido y lo compara con las zonas precargadas")
    void executeShouldMeasureChosenPointLiveWhenARequestCarriesAPoint() {
        givenDosZonasPrecargadasYUnEntornoMedible();

        whenSeExploraUnPuntoElegidoPorEscuelas();

        thenElPuntoElegidoSeMideYCompiteConLasZonas();
    }

    @Test
    @DisplayName("El punto elegido cuenta cada categoría y sólo la competencia del rubro")
    void executeShouldCountEveryCategoryWhenARequestCarriesAPoint() {
        givenUnaZonaBaseYUnEntornoMedible();

        whenSeExploraUnPuntoElegidoSinPreferencias();

        thenElPuntoMideCadaCategoriaYSoloLaCompetenciaDelRubro();
    }

    @Test
    @DisplayName("No mide un punto fuera de CABA y La Matanza")
    void executeShouldRejectWhenChosenPointIsOutsideCoverage() {
        givenUnaZonaBase();

        whenSeExploraUnPuntoFueraDeCobertura();

        thenSeRechazaPorFueraDeCoberturaSinConsultarElEntorno();
    }

    @Test
    @DisplayName("Sin punto elegido no consulta la fuente en vivo ni agrega zonas")
    void executeShouldNotQueryLiveSourceWhenThereIsNoChosenPoint() {
        givenUnaZonaConEscuelasMedidas(3);

        whenSeExploraConLosValoresPorDefecto();

        thenNoSeConsultaElEntornoNiSeAgreganZonas();
    }

    @ParameterizedTest(name = "rechaza {0}")
    @MethodSource("requestsInvalidos")
    @DisplayName("Rechaza filtros incompletos o inválidos")
    void executeShouldRejectWhenRequestIsIncompleteOrInvalid(String caso, ExplorarDondeAbrirRequest request) {
        whenSeIntentaExplorarElPedido(request);

        thenSeRechazaElPedido();
    }

    // --- given ---
    private void givenZonas(RubroNegocio rubro, List<DatosZonaDondeAbrir> zonas) {
        when(datosRepository.buscar(rubro)).thenReturn(zonas);
    }

    private void givenNegocio(Negocio negocio) {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
    }

    private void givenEntornoDelPunto() {
        when(entornoPort.consultar(eq(new BigDecimal("-34.65100")), eq(new BigDecimal("-58.55100")),
                eq(CLOCK.instant()))).thenReturn(entornoDelPunto());
    }

    private void givenDosZonasQueMidenUnoYDiez() {
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zona(ZONA_A, "Zona A", valores(1)), zona(ZONA_B, "Zona B", valores(10))));
    }

    private void givenCompetenciaBajaYAltaPara(RubroNegocio rubro) {
        givenZonas(rubro, List.of(
                zona(ZONA_A, "Poca", Map.of(FactorDondeAbrir.COMPETENCIA, medicion(1))),
                zona(ZONA_B, "Mucha", Map.of(FactorDondeAbrir.COMPETENCIA, medicion(10)))));
    }

    private void givenZonaQueExcedeTodosLosLimites() {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = new EnumMap<>(FactorDondeAbrir.class);
        metricas.put(FactorDondeAbrir.COMPETENCIA, medicion(10));
        metricas.put(FactorDondeAbrir.ESCUELAS, medicion(0));
        metricas.put(FactorDondeAbrir.PARQUES, medicion(0));
        metricas.put(FactorDondeAbrir.TRANSPORTE, medicion(0));
        givenZonas(RubroNegocio.ALMACEN, List.of(zona(ZONA_A, "Lejana", metricas)));
    }

    private void givenZonaCercanaConUnaFuenteVieja() {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = Map.of(
                FactorDondeAbrir.ACTIVIDAD_ACCESO,
                new DatosZonaDondeAbrir.Medicion(BigDecimal.TEN, "indice", OSM, LocalDate.parse("2026-01-01")));
        givenZonas(RubroNegocio.ALMACEN, List.of(new DatosZonaDondeAbrir(ZONA_A, "Cercana", "CABA", "CABA", "CABA",
                new BigDecimal("-34.6001"), new BigDecimal("-58.4001"), metricas, List.of(punto()))));
    }

    private void givenUnaZonaConEscuelasMedidas(int escuelas) {
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zona(ZONA_A, "Única", Map.of(FactorDondeAbrir.ESCUELAS, medicion(escuelas)))));
    }

    private void givenDosZonasConLasMismasEscuelasUnaLejosYUnaCerca() {
        var escuelas = Map.of(FactorDondeAbrir.ESCUELAS, medicion(3));
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zonaEn(ZONA_B, "Lejos", "-35.1", "-59.1", escuelas),
                zonaEn(ZONA_A, "Cerca", "-34.61", "-58.41", escuelas)));
    }

    private void givenDosZonasSinMetricas() {
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zona(ZONA_A, "Sin datos A", Map.of()), zona(ZONA_B, "Sin datos B", Map.of())));
    }

    private void givenDosZonasIgualesLlamadasPrimeraYSegunda() {
        var escuelas = Map.of(FactorDondeAbrir.ESCUELAS, medicion(3));
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zona(ZONA_A, "Primera", escuelas), zona(ZONA_B, "Segunda", escuelas)));
    }

    private void givenDosZonasSinMetricasUnaLejanaYUnaCercana() {
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zonaEn(ZONA_B, "Lejana", "-35.1", "-59.1", Map.of()),
                zonaEn(ZONA_A, "Cercana", "-34.61", "-58.41", Map.of())));
    }

    private void givenDosZonasQueCumplenLosLimites() {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = Map.of(
                FactorDondeAbrir.COMPETENCIA, medicion(2), FactorDondeAbrir.ESCUELAS, medicion(2));
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zona(ZONA_A, "Cotizada", metricas), zona(ZONA_B, "Sin cotizar", metricas)));
    }

    private void givenNegocioConCoordenadasIncompletas() {
        givenNegocio(Negocio.builder().nombre("Coordenadas incompletas").rubro(RubroNegocio.ALMACEN)
                .latitud(new BigDecimal("-34.6")).build());
    }

    private void givenUnaZonaSinFechaDeMedicion() {
        var sinFecha = new DatosZonaDondeAbrir.Medicion(BigDecimal.ONE, "lugares_500m", OSM, null);
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zona(ZONA_A, "Sin fecha", Map.of(FactorDondeAbrir.ESCUELAS, sinFecha))));
    }

    private void givenDosZonasPrecargadasYUnEntornoMedible() {
        givenZonas(RubroNegocio.ALMACEN, List.of(
                zonaEn(ZONA_A, "Base", "-34.65", "-58.55", Map.of(FactorDondeAbrir.ESCUELAS, medicion(1))),
                zonaEn(ZONA_B, "Lejana", "-34.70", "-58.60", Map.of(FactorDondeAbrir.ESCUELAS, medicion(0)))));
        givenEntornoDelPunto();
    }

    private void givenUnaZonaBaseYUnEntornoMedible() {
        givenUnaZonaBase();
        givenEntornoDelPunto();
    }

    private void givenUnaZonaBase() {
        givenZonas(RubroNegocio.ALMACEN, List.of(zonaEn(ZONA_A, "Base", "-34.65", "-58.55", Map.of())));
    }

    // --- when ---
    private void whenSeExploraElPedido(ExplorarDondeAbrirRequest pedido) {
        resultado = useCase.execute(pedido);
    }

    private void whenSeIntentaExplorarElPedido(ExplorarDondeAbrirRequest pedido) {
        error = catchThrowable(() -> useCase.execute(pedido));
    }

    private void whenSeExploraSoloPor(FactorDondeAbrir factor) {
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaSolo(factor), null, Map.of()));
    }

    private void whenSeExploraSoloPorConAlquileres(FactorDondeAbrir factor) {
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaSolo(factor), null, ALQUILERES));
    }

    private void whenSeExploraLaCompetenciaPara(RubroNegocio rubro) {
        whenSeExploraElPedido(request(rubro, PublicoDondeAbrir.OFICINAS,
                importanciaSolo(FactorDondeAbrir.COMPETENCIA), null, Map.of()));
    }

    private void whenSeExploraConTodosLosLimites() {
        var limites = new ExplorarDondeAbrirRequest.Limites(BigDecimal.ONE, 1, 2, 2, 2, BigDecimal.valueOf(500));
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.ESTUDIANTES,
                Map.of(), limites, Map.of(ZONA_A, BigDecimal.valueOf(1_000))));
    }

    private void whenSeExploraSinNingunaImportancia() {
        var limites = new ExplorarDondeAbrirRequest.Limites(BigDecimal.ONE, null, 0, 0, 0, null);
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaNinguna(), limites, Map.of()));
    }

    private void whenSeExploraSinNingunaImportanciaNiLimites() {
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaNinguna(), null, Map.of()));
    }

    private void whenSeExploraConLosValoresPorDefecto() {
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of()));
    }

    private void whenSeExploraConLimitesDeCompetenciaYEscuelas() {
        var limites = new ExplorarDondeAbrirRequest.Limites(null, 2, 1, null, null, null);
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of()));
    }

    private void whenSeExploraConUnLimiteDeCompetencia() {
        var limites = new ExplorarDondeAbrirRequest.Limites(null, 2, null, null, null, null);
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of()));
    }

    private void whenSeExploraConTodosLosLimitesYUnaSolaCotizacion() {
        var limites = new ExplorarDondeAbrirRequest.Limites(
                BigDecimal.valueOf(500), 2, 2, null, null, BigDecimal.valueOf(500));
        whenSeExploraElPedido(request(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of(ZONA_A, BigDecimal.valueOf(500))));
    }

    private void whenSeExploraConMapasNulos() {
        whenSeExploraElPedido(new ExplorarDondeAbrirRequest(
                USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, null, null, null));
    }

    private void whenSeExploraUnPuntoElegidoPorEscuelas() {
        whenSeExploraElPedido(new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN,
                PublicoDondeAbrir.ESTUDIANTES, importanciaSolo(FactorDondeAbrir.ESCUELAS), null, Map.of(),
                new ExplorarDondeAbrirRequest.PuntoElegido(new BigDecimal("-34.651"), new BigDecimal("-58.551"))));
    }

    private void whenSeExploraUnPuntoElegidoSinPreferencias() {
        whenSeExploraElPedido(new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN,
                PublicoDondeAbrir.VECINOS, Map.of(), null, Map.of(),
                new ExplorarDondeAbrirRequest.PuntoElegido(new BigDecimal("-34.651"), new BigDecimal("-58.551"))));
    }

    private void whenSeExploraUnPuntoFueraDeCobertura() {
        whenSeIntentaExplorarElPedido(new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN,
                PublicoDondeAbrir.VECINOS, Map.of(), null, Map.of(),
                new ExplorarDondeAbrirRequest.PuntoElegido(new BigDecimal("-31.4"), new BigDecimal("-64.2"))));
    }

    // --- then ---
    private void thenLaMejorZonaEs(String nombre, FactorDondeAbrir factor) {
        assertThat(resultado.ranking()).hasSize(2);
        var mejor = resultado.ranking().getFirst();
        var peor = resultado.ranking().getLast();
        assertThat(mejor.nombre()).isEqualTo(nombre);
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

    private void thenLasFrasesNombranA(String plural) {
        assertThat(resultado.ranking().getFirst().porQue().getFirst()).contains(plural);
        assertThat(resultado.ranking().getLast().riesgos().getFirst()).contains(plural);
    }

    private void thenSeDescartaLaZonaPorSeisMotivos() {
        assertThat(resultado.ranking()).isEmpty();
        assertThat(resultado.descartadas()).singleElement().satisfies(descartada ->
                assertThat(descartada.motivos()).hasSize(6));
    }

    private void thenLaZonaQuedaNeutraYSeAvisaQueFaltaLaUbicacion() {
        assertThat(resultado.ranking()).singleElement().satisfies(zona -> {
            assertThat(zona.distanciaKm()).isNull();
            assertThat(zona.puntaje()).isEqualByComparingTo("0");
            assertThat(zona.coincidencia()).isEqualTo("MALA");
            assertThat(zona.aportes()).isEmpty();
            assertThat(zona.porQue()).containsExactly("Ningún factor se destaca: es una zona pareja");
        });
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ubicación"));
    }

    private void thenSePenalizaLaZonaCercanaYSeAvisaDeLaFuenteVieja() {
        assertThat(resultado.ranking()).singleElement().satisfies(zona -> {
            assertThat(zona.puntaje()).isEqualByComparingTo("3.8");
            assertThat(zona.coincidencia()).isEqualTo("MALA");
            assertThat(zona.riesgos().getFirst()).contains("quitarse clientes");
            assertThat(zona.puntos()).hasSize(1);
            assertThat(zona.jurisdiccion()).isEqualTo("CABA");
        });
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("90 días"));
    }

    private void thenLaZonaQuedaIntermediaConUnRiesgoGenerico() {
        var zona = resultado.ranking().getFirst();
        assertThat(zona.puntaje()).isEqualByComparingTo("5");
        assertThat(zona.coincidencia()).isEqualTo("INTERMEDIA");
        assertThat(zona.riesgos())
                .containsExactly("Son datos estimados: confirmalos caminando la zona antes de decidir");
    }

    private void thenElRankingEs(String primera, String segunda) {
        assertThat(resultado.ranking()).extracting(ExplorarDondeAbrirResponse.ZonaEvaluada::nombre)
                .containsExactly(primera, segunda);
    }

    private void thenLasDescartadasSon(String primera, String segunda) {
        assertThat(resultado.descartadas()).extracting(ExplorarDondeAbrirResponse.ZonaDescartada::nombre)
                .containsExactly(primera, segunda);
    }

    private void thenSeDescartanLasDosZonasPorFaltaDeDatos() {
        assertThat(resultado.ranking()).isEmpty();
        assertThat(resultado.descartadas()).hasSize(2).allSatisfy(descartada -> {
            assertThat(descartada.distanciaKm()).isNull();
            assertThat(descartada.motivos()).hasSize(2)
                    .allMatch(motivo -> motivo.startsWith("No hay datos de"));
        });
    }

    private void thenNingunaZonaSeDescarta() {
        assertThat(resultado.ranking()).hasSize(2);
        assertThat(resultado.descartadas()).isEmpty();
    }

    private void thenElUnicoFactorPesaTodo() {
        assertThat(resultado.ranking().getFirst().aportes()).singleElement().satisfies(aporte ->
                assertThat(aporte.peso()).isEqualByComparingTo("1"));
    }

    private void thenNoHayDistanciaNiAvisoDeFuenteVieja() {
        assertThat(resultado.ranking().getFirst().distanciaKm()).isNull();
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ubicación"))
                .noneMatch(aviso -> aviso.contains("90 días"));
    }

    private void thenCadaZonaTieneComoMuchoDosRiesgosYAlgunaTieneDos() {
        assertThat(resultado.ranking()).allSatisfy(zona -> assertThat(zona.riesgos()).hasSizeLessThanOrEqualTo(2));
        assertThat(resultado.ranking()).anySatisfy(zona -> assertThat(zona.riesgos()).hasSize(2));
    }

    private void thenElPuntoElegidoSeMideYCompiteConLasZonas() {
        assertThat(resultado.evaluadas()).isEqualTo(3);
        assertThat(resultado.puntoElegidoId()).isNotNull();
        var punto = resultado.ranking().getFirst();
        assertThat(punto.id()).isEqualTo(resultado.puntoElegidoId());
        assertThat(punto.nombre()).isEqualTo("Punto elegido · cerca de Base");
        assertThat(punto.jurisdiccion()).isEqualTo("La Matanza");
        assertThat(punto.aportes()).singleElement().satisfies(aporte -> {
            assertThat(aporte.factor()).isEqualTo(FactorDondeAbrir.ESCUELAS);
            assertThat(aporte.valor()).isEqualByComparingTo("2");
            assertThat(aporte.fuente().codigo()).isEqualTo("OSM");
            assertThat(aporte.observadaEn()).isEqualTo(LocalDate.parse("2026-11-10"));
        });
        assertThat(punto.puntos()).hasSize(8).noneMatch(lugar -> "KIOSCO".equals(lugar.rubro()));
        assertThat(resultado.avisos().getFirst()).contains("OpenStreetMap");
    }

    private void thenElPuntoMideCadaCategoriaYSoloLaCompetenciaDelRubro() {
        var aportes = resultado.ranking().stream()
                .filter(zona -> zona.id().equals(resultado.puntoElegidoId())).findFirst().orElseThrow().aportes();
        assertThat(aportes).extracting(ExplorarDondeAbrirResponse.Aporte::factor).containsExactlyInAnyOrder(
                FactorDondeAbrir.COMPETENCIA, FactorDondeAbrir.ESCUELAS, FactorDondeAbrir.OFICINAS,
                FactorDondeAbrir.TRANSPORTE, FactorDondeAbrir.PARQUES, FactorDondeAbrir.SALUD,
                FactorDondeAbrir.COMPLEMENTARIOS);
        assertThat(aportes).filteredOn(aporte -> aporte.factor() == FactorDondeAbrir.COMPETENCIA)
                .singleElement().satisfies(aporte -> assertThat(aporte.valor()).isEqualByComparingTo("1"));
    }

    private void thenSeRechazaPorFueraDeCoberturaSinConsultarElEntorno() {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessageContaining("CABA y La Matanza");
        verifyNoInteractions(entornoPort);
    }

    private void thenNoSeConsultaElEntornoNiSeAgreganZonas() {
        assertThat(resultado.puntoElegidoId()).isNull();
        assertThat(resultado.evaluadas()).isEqualTo(1);
        verifyNoInteractions(entornoPort);
    }

    private void thenSeRechazaElPedido() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    // --- fixtures ---
    private static Map<FactorDondeAbrir, Integer> importanciaNinguna() {
        Map<FactorDondeAbrir, Integer> importancia = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            importancia.put(factor, 0);
        }
        return importancia;
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
                new Object[]{"punto sin latitud", conPunto(null, BigDecimal.ONE)},
                new Object[]{"punto sin longitud", conPunto(BigDecimal.ONE, null)},
                new Object[]{"latitud fuera de rango", conPunto(new BigDecimal("91"), BigDecimal.ONE)},
                new Object[]{"longitud fuera de rango", conPunto(BigDecimal.ONE, new BigDecimal("-181"))},
                new Object[]{"alquiler máximo negativo", conLimites(BigDecimal.ZERO, 0, 0, 0, 0,
                        new BigDecimal("-1"))});
    }

    private static ExplorarDondeAbrirRequest conPunto(BigDecimal latitud, BigDecimal longitud) {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of(), new ExplorarDondeAbrirRequest.PuntoElegido(latitud, longitud));
    }

    private static List<DatosZonaDondeAbrir.Punto> entornoDelPunto() {
        return List.of(
                lugar("ESCUELA", null), lugar("ESCUELA", null), lugar("OFICINA", null),
                lugar("TRANSPORTE", null), lugar("PARQUE", null), lugar("SALUD", null),
                lugar("COMPLEMENTARIO", null), lugar("COMPETENCIA", "ALMACEN"),
                lugar("COMPETENCIA", "KIOSCO"));
    }

    private static DatosZonaDondeAbrir.Punto lugar(String tipo, String rubro) {
        return new DatosZonaDondeAbrir.Punto("node:" + tipo + rubro, tipo, rubro, "Lugar",
                new BigDecimal("-34.651"), new BigDecimal("-58.551"), OSM, LocalDate.parse("2026-11-10"));
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
            importancia.put(factor, 0);
        }
        importancia.put(elegido, 2);
        return importancia;
    }

    private static Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> valores(int valor) {
        Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            if (factor != FactorDondeAbrir.ALQUILER) {
                DatosZonaDondeAbrir.Fuente fuente = Map.of(FactorDondeAbrir.DEMANDA_ENSAMBLA, ENSAMBLA).getOrDefault(factor, OSM);
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
