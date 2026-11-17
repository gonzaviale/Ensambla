package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.DondeAbrirDePrueba.RELOJ;
import static com.ensambla.backend.support.DondeAbrirDePrueba.USUARIO_ID;
import static com.ensambla.backend.support.DondeAbrirDePrueba.ZONA_A;
import static com.ensambla.backend.support.DondeAbrirDePrueba.ZONA_B;
import static com.ensambla.backend.support.DondeAbrirDePrueba.importanciaNinguna;
import static com.ensambla.backend.support.DondeAbrirDePrueba.importanciaSoloPara;
import static com.ensambla.backend.support.DondeAbrirDePrueba.medicionesDeTodosLosFactoresEn;
import static com.ensambla.backend.support.DondeAbrirDePrueba.medicionesQueExcedenTodosLosLimites;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unEntornoDelPunto;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unNegocioConCoordenadasIncompletas;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unNegocioConUbicacion;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unNegocioSinUbicacion;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unPedido;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unPedidoConMapasNulos;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unPedidoConPunto;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unaMedicionDe;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unaMedicionSinFecha;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unaZona;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unaZonaEn;
import static com.ensambla.backend.support.DondeAbrirDePrueba.unaZonaPegadaAlNegocioConUnaFuenteVieja;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExplorarDondeAbrirTest {

    private static final Map<ZonaId, BigDecimal> ALQUILERES = Map.of(ZONA_A, BigDecimal.ONE, ZONA_B, BigDecimal.TEN);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final DatosDondeAbrirRepository datosRepository = mock(DatosDondeAbrirRepository.class);
    private final EntornoDelPuntoPort entornoPort = mock(EntornoDelPuntoPort.class);
    private final ExplorarDondeAbrir explorarDondeAbrir =
            new ExplorarDondeAbrir(negocioRepository, datosRepository, entornoPort, RELOJ);

    private ExplorarDondeAbrirResponse resultado;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioConUbicacion() {
        dadoElNegocio(unNegocioConUbicacion());
    }

    @ParameterizedTest(name = "explica el factor {0}")
    @EnumSource(value = FactorDondeAbrir.class, names = {"COMPETENCIA", "ALQUILER"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Cuando más es mejor, la zona con el valor más alto encabeza el ranking")
    void cuandoMasEsMejorLaZonaConElValorMasAltoEncabezaElRanking(FactorDondeAbrir factor) {
        dadoDosZonasQueMidenUnoYDiez();

        cuandoSeExploraSoloPorConAlquileres(factor);

        entoncesLaMejorZonaEs("Zona B", factor);
    }

    @ParameterizedTest(name = "explica el factor {0}")
    @EnumSource(value = FactorDondeAbrir.class, names = {"COMPETENCIA", "ALQUILER"})
    @DisplayName("Cuando menos es mejor, la zona con el valor más bajo encabeza el ranking")
    void cuandoMenosEsMejorLaZonaConElValorMasBajoEncabezaElRanking(FactorDondeAbrir factor) {
        dadoDosZonasQueMidenUnoYDiez();

        cuandoSeExploraSoloPorConAlquileres(factor);

        entoncesLaMejorZonaEs("Zona A", factor);
    }

    @ParameterizedTest
    @CsvSource({"PAPELERIA,papelerías", "KIOSCO,kioscos"})
    @DisplayName("Usa los pesos y nombres propios de papelería y kiosco")
    void lasFrasesNombranElRubroDePapeleriaYKiosco(RubroNegocio rubro, String plural) {
        dadoCompetenciaBajaYAltaPara(rubro);

        cuandoSeExploraLaCompetenciaPara(rubro);

        entoncesLasFrasesNombranA(plural);
    }

    @Test
    @DisplayName("Aplica todos los límites antes de puntuar")
    void unaZonaQueExcedeTodosLosLimitesSeDescartaAntesDePuntuar() {
        dadoUnaZonaQueExcedeTodosLosLimites();

        cuandoSeExploraConTodosLosLimites();

        entoncesSeDescartaLaZonaPorSeisMotivos();
    }

    @Test
    @DisplayName("Informa cuando falta ubicación y tolera una zona sin métricas ni factores elegidos")
    void sinUbicacionDelNegocioSeAvisaYLaZonaSinDatosQuedaNeutra() {
        dadoUnNegocioSinUbicacion();
        dadoUnaZonaSinMetricas();

        cuandoSeExploraSinNingunaImportancia();

        entoncesLaZonaQuedaNeutraYSeAvisaQueFaltaLaUbicacion();
    }

    @Test
    @DisplayName("Penaliza una zona a menos de un kilómetro y avisa si una fuente está desactualizada")
    void unaZonaAMenosDeUnKilometroSePenalizaYSeAvisaDeLaFuenteVieja() {
        dadoUnaZonaPegadaAlNegocioConUnaFuenteVieja();

        cuandoSeExploraConLosValoresPorDefecto();

        entoncesSePenalizaLaZonaCercanaYSeAvisaDeLaFuenteVieja();
    }

    @Test
    @DisplayName("Una zona con un único valor comparable obtiene un subpuntaje intermedio")
    void unValorUnicoComparableDaUnSubpuntajeIntermedio() {
        dadoUnaZonaConEscuelasMedidas(3);

        cuandoSeExploraSoloPor(FactorDondeAbrir.ESCUELAS);

        entoncesLaZonaQuedaIntermediaConUnRiesgoGenerico();
    }

    @Test
    @DisplayName("Desempata por distancia cuando dos zonas tienen el mismo puntaje")
    void conElMismoPuntajeSeDesempataPorDistancia() {
        dadoDosZonasConLasMismasEscuelasUnaLejosYUnaCerca();

        cuandoSeExploraSoloPor(FactorDondeAbrir.ESCUELAS);

        entoncesElRankingEs("Cerca", "Lejos");
    }

    @Test
    @DisplayName("Descarta y ordena zonas sin distancia cuando faltan las métricas exigidas")
    void sinMetricasExigidasNiUbicacionSeDescartanLasZonasSinDistancia() {
        dadoUnNegocioSinUbicacion();
        dadoDosZonasSinMetricas();

        cuandoSeExploraConLimitesDeCompetenciaYEscuelas();

        entoncesSeDescartanLasDosZonasPorFaltaDeDatos();
    }

    @Test
    @DisplayName("Conserva el orden si hay empate y el negocio todavía no tiene ubicación")
    void conEmpateYSinUbicacionSeConservaElOrdenOriginal() {
        dadoUnNegocioSinUbicacion();
        dadoDosZonasIgualesLlamadasPrimeraYSegunda();

        cuandoSeExploraSoloPor(FactorDondeAbrir.ESCUELAS);

        entoncesElRankingEs("Primera", "Segunda");
    }

    @Test
    @DisplayName("Ordena las zonas descartadas desde la más cercana")
    void lasZonasDescartadasSeOrdenanDesdeLaMasCercana() {
        dadoDosZonasSinMetricasUnaLejanaYUnaCercana();

        cuandoSeExploraConUnLimiteDeCompetencia();

        entoncesLasDescartadasSon("Cercana", "Lejana");
    }

    @Test
    @DisplayName("Conserva zonas que cumplen los límites, tengan o no una cotización de alquiler")
    void lasZonasQueCumplenLosLimitesSeConservanConOSinCotizacion() {
        dadoDosZonasQueCumplenLosLimites();

        cuandoSeExploraConTodosLosLimitesYUnaSolaCotizacion();

        entoncesNingunaZonaSeDescarta();
    }

    @Test
    @DisplayName("Reparte el peso por igual si la persona desactiva todos los factores disponibles")
    void sinFactoresActivosElPesoSeRepartePorIgual() {
        dadoUnaZonaConEscuelasMedidas(3);

        cuandoSeExploraSinNingunaImportanciaNiLimites();

        entoncesElUnicoFactorPesaTodo();
    }

    @Test
    @DisplayName("Tolera mapas nulos, una coordenada incompleta y una fecha de medición desconocida")
    void seToleranMapasNulosCoordenadasIncompletasYFechasDesconocidas() {
        dadoUnNegocioConCoordenadasIncompletas();
        dadoUnaZonaSinFechaDeMedicion();

        cuandoSeExploraConMapasNulos();

        entoncesNoHayDistanciaNiAvisoDeFuenteVieja();
    }

    @Test
    @DisplayName("Ordena varios riesgos y omite los factores con peso insignificante")
    void variosFactoresDebilesDejanComoMuchoDosRiesgos() {
        dadoDosZonasQueMidenUnoYDiez();

        cuandoSeExploraConLosValoresPorDefecto();

        entoncesCadaZonaTieneComoMuchoDosRiesgosYAlgunaTieneDos();
    }

    @Test
    @DisplayName("Mide en vivo el punto elegido y lo compara con las zonas precargadas")
    void elPuntoElegidoSeMideEnVivoYCompiteConLasZonasPrecargadas() {
        dadoDosZonasPrecargadasYUnEntornoMedible();

        cuandoSeExploraUnPuntoElegidoPorEscuelas();

        entoncesElPuntoElegidoSeMideYCompiteConLasZonas();
    }

    @Test
    @DisplayName("El punto elegido cuenta cada categoría y sólo la competencia del rubro")
    void elPuntoElegidoCuentaCadaCategoriaYSoloLaCompetenciaDelRubro() {
        dadoUnaZonaBaseYUnEntornoMedible();

        cuandoSeExploraUnPuntoElegidoSinPreferencias();

        entoncesElPuntoMideCadaCategoriaYSoloLaCompetenciaDelRubro();
    }

    @Test
    @DisplayName("No mide un punto fuera de CABA y La Matanza")
    void unPuntoFueraDeCabaYLaMatanzaSeRechaza() {
        dadoUnaZonaBase();

        cuandoSeIntentaExplorarUnPuntoFueraDeCobertura();

        entoncesSeRechazaPorFueraDeCoberturaSinConsultarElEntorno();
    }

    @Test
    @DisplayName("Sin punto elegido no consulta la fuente en vivo ni agrega zonas")
    void sinPuntoElegidoNoSeConsultaLaFuenteEnVivo() {
        dadoUnaZonaConEscuelasMedidas(3);

        cuandoSeExploraConLosValoresPorDefecto();

        entoncesNoSeConsultaElEntornoNiSeAgreganZonas();
    }

    @ParameterizedTest(name = "rechaza {0}")
    @MethodSource("com.ensambla.backend.support.DondeAbrirDePrueba#pedidosInvalidos")
    @DisplayName("Rechaza filtros incompletos o inválidos")
    void sePidenFiltrosIncompletosOInvalidosYSeRechaza(String caso, ExplorarDondeAbrirRequest pedido) {
        cuandoSeIntentaExplorarElPedido(pedido);

        entoncesSeRechazaElPedido();
    }

    // --- dado ---
    private void dadoLasZonas(RubroNegocio rubro, List<DatosZonaDondeAbrir> zonas) {
        when(datosRepository.buscar(rubro)).thenReturn(zonas);
    }

    private void dadoElNegocio(Negocio negocio) {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
    }

    private void dadoElEntornoDelPunto() {
        when(entornoPort.consultar(eq(new BigDecimal("-34.65100")), eq(new BigDecimal("-58.55100")),
                eq(RELOJ.instant()))).thenReturn(unEntornoDelPunto());
    }

    private void dadoUnNegocioSinUbicacion() {
        dadoElNegocio(unNegocioSinUbicacion());
    }

    private void dadoUnNegocioConCoordenadasIncompletas() {
        dadoElNegocio(unNegocioConCoordenadasIncompletas());
    }

    private void dadoDosZonasQueMidenUnoYDiez() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZona(ZONA_A, "Zona A", medicionesDeTodosLosFactoresEn(1)),
                unaZona(ZONA_B, "Zona B", medicionesDeTodosLosFactoresEn(10))));
    }

    private void dadoCompetenciaBajaYAltaPara(RubroNegocio rubro) {
        dadoLasZonas(rubro, List.of(
                unaZona(ZONA_A, "Poca", Map.of(FactorDondeAbrir.COMPETENCIA, unaMedicionDe(1))),
                unaZona(ZONA_B, "Mucha", Map.of(FactorDondeAbrir.COMPETENCIA, unaMedicionDe(10)))));
    }

    private void dadoUnaZonaQueExcedeTodosLosLimites() {
        dadoLasZonas(RubroNegocio.ALMACEN,
                List.of(unaZona(ZONA_A, "Lejana", medicionesQueExcedenTodosLosLimites())));
    }

    private void dadoUnaZonaSinMetricas() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(unaZona(ZONA_A, "Sin datos", Map.of())));
    }

    private void dadoUnaZonaPegadaAlNegocioConUnaFuenteVieja() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(unaZonaPegadaAlNegocioConUnaFuenteVieja()));
    }

    private void dadoUnaZonaConEscuelasMedidas(int escuelas) {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZona(ZONA_A, "Única", Map.of(FactorDondeAbrir.ESCUELAS, unaMedicionDe(escuelas)))));
    }

    private void dadoDosZonasConLasMismasEscuelasUnaLejosYUnaCerca() {
        var escuelas = Map.of(FactorDondeAbrir.ESCUELAS, unaMedicionDe(3));
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZonaEn(ZONA_B, "Lejos", "-35.1", "-59.1", escuelas),
                unaZonaEn(ZONA_A, "Cerca", "-34.61", "-58.41", escuelas)));
    }

    private void dadoDosZonasSinMetricas() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZona(ZONA_A, "Sin datos A", Map.of()), unaZona(ZONA_B, "Sin datos B", Map.of())));
    }

    private void dadoDosZonasIgualesLlamadasPrimeraYSegunda() {
        var escuelas = Map.of(FactorDondeAbrir.ESCUELAS, unaMedicionDe(3));
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZona(ZONA_A, "Primera", escuelas), unaZona(ZONA_B, "Segunda", escuelas)));
    }

    private void dadoDosZonasSinMetricasUnaLejanaYUnaCercana() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZonaEn(ZONA_B, "Lejana", "-35.1", "-59.1", Map.of()),
                unaZonaEn(ZONA_A, "Cercana", "-34.61", "-58.41", Map.of())));
    }

    private void dadoDosZonasQueCumplenLosLimites() {
        var metricas = Map.of(
                FactorDondeAbrir.COMPETENCIA, unaMedicionDe(2), FactorDondeAbrir.ESCUELAS, unaMedicionDe(2));
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZona(ZONA_A, "Cotizada", metricas), unaZona(ZONA_B, "Sin cotizar", metricas)));
    }

    private void dadoUnaZonaSinFechaDeMedicion() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZona(ZONA_A, "Sin fecha", Map.of(FactorDondeAbrir.ESCUELAS, unaMedicionSinFecha()))));
    }

    private void dadoDosZonasPrecargadasYUnEntornoMedible() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(
                unaZonaEn(ZONA_A, "Base", "-34.65", "-58.55", Map.of(FactorDondeAbrir.ESCUELAS, unaMedicionDe(1))),
                unaZonaEn(ZONA_B, "Lejana", "-34.70", "-58.60",
                        Map.of(FactorDondeAbrir.ESCUELAS, unaMedicionDe(0)))));
        dadoElEntornoDelPunto();
    }

    private void dadoUnaZonaBaseYUnEntornoMedible() {
        dadoUnaZonaBase();
        dadoElEntornoDelPunto();
    }

    private void dadoUnaZonaBase() {
        dadoLasZonas(RubroNegocio.ALMACEN, List.of(unaZonaEn(ZONA_A, "Base", "-34.65", "-58.55", Map.of())));
    }

    // --- cuando ---
    private void cuandoSeExploraElPedido(ExplorarDondeAbrirRequest pedido) {
        resultado = explorarDondeAbrir.execute(pedido);
    }

    private void cuandoSeIntentaExplorarElPedido(ExplorarDondeAbrirRequest pedido) {
        rechazo = catchThrowable(() -> explorarDondeAbrir.execute(pedido));
    }

    private void cuandoSeExploraSoloPor(FactorDondeAbrir factor) {
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaSoloPara(factor), null, Map.of()));
    }

    private void cuandoSeExploraSoloPorConAlquileres(FactorDondeAbrir factor) {
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaSoloPara(factor), null, ALQUILERES));
    }

    private void cuandoSeExploraLaCompetenciaPara(RubroNegocio rubro) {
        cuandoSeExploraElPedido(unPedido(rubro, PublicoDondeAbrir.OFICINAS,
                importanciaSoloPara(FactorDondeAbrir.COMPETENCIA), null, Map.of()));
    }

    private void cuandoSeExploraConTodosLosLimites() {
        var limites = new ExplorarDondeAbrirRequest.Limites(BigDecimal.ONE, 1, 2, 2, 2, BigDecimal.valueOf(500));
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.ESTUDIANTES,
                Map.of(), limites, Map.of(ZONA_A, BigDecimal.valueOf(1_000))));
    }

    private void cuandoSeExploraSinNingunaImportancia() {
        var limites = new ExplorarDondeAbrirRequest.Limites(BigDecimal.ONE, null, 0, 0, 0, null);
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaNinguna(), limites, Map.of()));
    }

    private void cuandoSeExploraSinNingunaImportanciaNiLimites() {
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                importanciaNinguna(), null, Map.of()));
    }

    private void cuandoSeExploraConLosValoresPorDefecto() {
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of()));
    }

    private void cuandoSeExploraConLimitesDeCompetenciaYEscuelas() {
        var limites = new ExplorarDondeAbrirRequest.Limites(null, 2, 1, null, null, null);
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of()));
    }

    private void cuandoSeExploraConUnLimiteDeCompetencia() {
        var limites = new ExplorarDondeAbrirRequest.Limites(null, 2, null, null, null, null);
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of()));
    }

    private void cuandoSeExploraConTodosLosLimitesYUnaSolaCotizacion() {
        var limites = new ExplorarDondeAbrirRequest.Limites(
                BigDecimal.valueOf(500), 2, 2, null, null, BigDecimal.valueOf(500));
        cuandoSeExploraElPedido(unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), limites, Map.of(ZONA_A, BigDecimal.valueOf(500))));
    }

    private void cuandoSeExploraConMapasNulos() {
        cuandoSeExploraElPedido(unPedidoConMapasNulos());
    }

    private void cuandoSeExploraUnPuntoElegidoPorEscuelas() {
        cuandoSeExploraElPedido(unPedidoConPunto(PublicoDondeAbrir.ESTUDIANTES,
                importanciaSoloPara(FactorDondeAbrir.ESCUELAS), "-34.651", "-58.551"));
    }

    private void cuandoSeExploraUnPuntoElegidoSinPreferencias() {
        cuandoSeExploraElPedido(unPedidoConPunto(PublicoDondeAbrir.VECINOS, Map.of(), "-34.651", "-58.551"));
    }

    private void cuandoSeIntentaExplorarUnPuntoFueraDeCobertura() {
        cuandoSeIntentaExplorarElPedido(unPedidoConPunto(PublicoDondeAbrir.VECINOS, Map.of(), "-31.4", "-64.2"));
    }

    // --- entonces ---
    private void entoncesLaMejorZonaEs(String nombre, FactorDondeAbrir factor) {
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

    private void entoncesLasFrasesNombranA(String plural) {
        assertThat(resultado.ranking().getFirst().porQue().getFirst()).contains(plural);
        assertThat(resultado.ranking().getLast().riesgos().getFirst()).contains(plural);
    }

    private void entoncesSeDescartaLaZonaPorSeisMotivos() {
        assertThat(resultado.ranking()).isEmpty();
        assertThat(resultado.descartadas()).singleElement().satisfies(descartada ->
                assertThat(descartada.motivos()).hasSize(6));
    }

    private void entoncesLaZonaQuedaNeutraYSeAvisaQueFaltaLaUbicacion() {
        assertThat(resultado.ranking()).singleElement().satisfies(zona -> {
            assertThat(zona.distanciaKm()).isNull();
            assertThat(zona.puntaje()).isEqualByComparingTo("0");
            assertThat(zona.coincidencia()).isEqualTo("MALA");
            assertThat(zona.aportes()).isEmpty();
            assertThat(zona.porQue()).containsExactly("Ningún factor se destaca: es una zona pareja");
        });
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ubicación"));
    }

    private void entoncesSePenalizaLaZonaCercanaYSeAvisaDeLaFuenteVieja() {
        assertThat(resultado.ranking()).singleElement().satisfies(zona -> {
            assertThat(zona.puntaje()).isEqualByComparingTo("3.8");
            assertThat(zona.coincidencia()).isEqualTo("MALA");
            assertThat(zona.riesgos().getFirst()).contains("quitarse clientes");
            assertThat(zona.puntos()).hasSize(1);
            assertThat(zona.jurisdiccion()).isEqualTo("CABA");
        });
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("90 días"));
    }

    private void entoncesLaZonaQuedaIntermediaConUnRiesgoGenerico() {
        var zona = resultado.ranking().getFirst();
        assertThat(zona.puntaje()).isEqualByComparingTo("5");
        assertThat(zona.coincidencia()).isEqualTo("INTERMEDIA");
        assertThat(zona.riesgos())
                .containsExactly("Son datos estimados: confirmalos caminando la zona antes de decidir");
    }

    private void entoncesElRankingEs(String primera, String segunda) {
        assertThat(resultado.ranking()).extracting(ExplorarDondeAbrirResponse.ZonaEvaluada::nombre)
                .containsExactly(primera, segunda);
    }

    private void entoncesLasDescartadasSon(String primera, String segunda) {
        assertThat(resultado.descartadas()).extracting(ExplorarDondeAbrirResponse.ZonaDescartada::nombre)
                .containsExactly(primera, segunda);
    }

    private void entoncesSeDescartanLasDosZonasPorFaltaDeDatos() {
        assertThat(resultado.ranking()).isEmpty();
        assertThat(resultado.descartadas()).hasSize(2).allSatisfy(descartada -> {
            assertThat(descartada.distanciaKm()).isNull();
            assertThat(descartada.motivos()).hasSize(2)
                    .allMatch(motivo -> motivo.startsWith("No hay datos de"));
        });
    }

    private void entoncesNingunaZonaSeDescarta() {
        assertThat(resultado.ranking()).hasSize(2);
        assertThat(resultado.descartadas()).isEmpty();
    }

    private void entoncesElUnicoFactorPesaTodo() {
        assertThat(resultado.ranking().getFirst().aportes()).singleElement().satisfies(aporte ->
                assertThat(aporte.peso()).isEqualByComparingTo("1"));
    }

    private void entoncesNoHayDistanciaNiAvisoDeFuenteVieja() {
        assertThat(resultado.ranking().getFirst().distanciaKm()).isNull();
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ubicación"))
                .noneMatch(aviso -> aviso.contains("90 días"));
    }

    private void entoncesCadaZonaTieneComoMuchoDosRiesgosYAlgunaTieneDos() {
        assertThat(resultado.ranking()).allSatisfy(zona -> assertThat(zona.riesgos()).hasSizeLessThanOrEqualTo(2));
        assertThat(resultado.ranking()).anySatisfy(zona -> assertThat(zona.riesgos()).hasSize(2));
    }

    private void entoncesElPuntoElegidoSeMideYCompiteConLasZonas() {
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

    private void entoncesElPuntoMideCadaCategoriaYSoloLaCompetenciaDelRubro() {
        var aportes = resultado.ranking().stream()
                .filter(zona -> zona.id().equals(resultado.puntoElegidoId())).findFirst().orElseThrow().aportes();
        assertThat(aportes).extracting(ExplorarDondeAbrirResponse.Aporte::factor).containsExactlyInAnyOrder(
                FactorDondeAbrir.COMPETENCIA, FactorDondeAbrir.ESCUELAS, FactorDondeAbrir.OFICINAS,
                FactorDondeAbrir.TRANSPORTE, FactorDondeAbrir.PARQUES, FactorDondeAbrir.SALUD,
                FactorDondeAbrir.COMPLEMENTARIOS);
        assertThat(aportes).filteredOn(aporte -> aporte.factor() == FactorDondeAbrir.COMPETENCIA)
                .singleElement().satisfies(aporte -> assertThat(aporte.valor()).isEqualByComparingTo("1"));
    }

    private void entoncesSeRechazaPorFueraDeCoberturaSinConsultarElEntorno() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining("CABA y La Matanza");
        verifyNoInteractions(entornoPort);
    }

    private void entoncesNoSeConsultaElEntornoNiSeAgreganZonas() {
        assertThat(resultado.puntoElegidoId()).isNull();
        assertThat(resultado.evaluadas()).isEqualTo(1);
        verifyNoInteractions(entornoPort);
    }

    private void entoncesSeRechazaElPedido() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }
}
