package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import com.ensambla.backend.support.NegocioDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.NegocioDePrueba.YERBA;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerProgresoTest {

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);
    /** El día argentino de AHORA, un martes. */
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final LocalDate ESTA_SEMANA = LocalDate.of(2026, 11, 9);
    private static final LocalDate SEMANA_ANTERIOR = LocalDate.of(2026, 11, 2);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final ProgresoObjetivoRepository progresoObjetivoRepository = mock(ProgresoObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final ResumenSemanalRepository resumenSemanalRepository = mock(ResumenSemanalRepository.class);
    private ObtenerProgreso obtenerProgreso;
    private ObtenerProgresoResponse progreso;
    private Throwable error;

    @BeforeEach
    void setUp() {
        obtenerProgreso = new ObtenerProgreso(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                progresoObjetivoRepository,
                propuestaMiniObjetivosService,
                resumenSemanalRepository,
                CLOCK);
        givenUnNegocioConUnObjetivoConVentas();
    }

    @Test
    @DisplayName("Devuelve la meta del objetivo")
    void executeShouldReturnTheGoalOfTheObjetivo() {
        givenFotos(foto(null, HOY, "-27000"));
        givenMinis();

        whenSeObtieneElProgreso();

        thenLaMetaEsLaDelObjetivo();
    }

    @Test
    @DisplayName("Devuelve la serie semanal medida con la última foto de cada semana")
    void executeShouldReturnTheMeasuredWeeklySeriesOfTheObjetivo() {
        givenFotos(foto(null, HOY.minusDays(1), "-30000"), foto(null, HOY, "-27000"));
        givenMinis();

        whenSeObtieneElProgreso();

        thenLaSerieMedidaTieneLaUltimaFotoDeEstaSemana();
    }

    @Test
    @DisplayName("Completa las semanas anteriores a la primera foto con el resultado calculado, marcadas como estimadas")
    void executeShouldCompleteThePreviousWeeksWithEstimates() {
        givenFotos(foto(null, HOY, "-27000"));
        givenMinis();

        whenSeObtieneElProgreso();

        thenLasSemanasAnterioresALaPrimeraFotoVanEstimadas();
    }

    @Test
    @DisplayName("Una semana ya cerrada muestra el resultado de su cierre del domingo, igual que su resumen")
    void executeShouldShowTheSundayCloseResultForAClosedWeek() {
        givenFotos(foto(null, HOY, "-27000"));
        givenMinis();
        givenCierre(SEMANA_ANTERIOR, "-31000");

        whenSeObtieneElProgreso();

        thenLaSemanaAnteriorMuestraElResultadoDeSuCierre();
    }

    @Test
    @DisplayName("Sin ninguna foto todo lo que se sabe del pasado va estimado")
    void executeShouldShowEverythingAsEstimatedWithoutPhotos() {
        givenFotos();
        givenMinis();

        whenSeObtieneElProgreso();

        thenTodasLasSemanasVanEstimadas();
    }

    @Test
    @DisplayName("Sin ventas ni fotos la serie del objetivo está vacía")
    void executeShouldReturnAnEmptySeriesWithoutSalesOrPhotos() {
        givenUnNegocioSinVentas();
        givenFotos();
        givenMinis();

        whenSeObtieneElProgreso();

        thenLaSerieDelObjetivoEstaVacia();
    }

    @Test
    @DisplayName("Sin ventas ni fotos no hay series de mini-objetivos")
    void executeShouldReturnNoMiniSeriesWithoutSalesOrPhotos() {
        givenUnNegocioSinVentas();
        givenFotos();
        givenMinis();

        whenSeObtieneElProgreso();

        thenNoHaySeriesDeMinis();
    }

    @Test
    @DisplayName("Sólo traen serie los mini-objetivos en curso o logrados: los reemplazados o quitados no")
    void executeShouldReturnTheSeriesOfTheMiniObjetivosInProgressOrAchieved() {
        givenFotos(foto(MINI_ID, HOY, "12000"), foto(OTRO_MINI_ID, HOY, "14500"), foto(null, HOY, "-27000"));
        givenMinis(miniObjetivo(), miniLogrado(), miniReemplazado());

        whenSeObtieneElProgreso();

        thenSoloTraenSerieLosMinisEnCursoOLogrados();
    }

    @Test
    @DisplayName("Cada mini-objetivo trae las fotos de su propia serie")
    void executeShouldReturnThePhotosOfEachMiniObjetivo() {
        givenFotos(foto(MINI_ID, HOY, "12000"), foto(OTRO_MINI_ID, HOY, "14500"), foto(null, HOY, "-27000"));
        givenMinis(miniObjetivo(), miniLogrado());

        whenSeObtieneElProgreso();

        thenCadaMiniTraeSuPropiaSerie();
    }

    @Test
    @DisplayName("Un mini-objetivo logrado trae el hito de logrado y uno en curso no trae hito")
    void executeShouldReturnTheMilestoneOfAnAchievedMiniObjetivo() {
        givenFotos(foto(MINI_ID, HOY, "12000"), foto(OTRO_MINI_ID, HOY, "14500"));
        givenMinis(miniObjetivo(), miniLogrado());

        whenSeObtieneElProgreso();

        thenSoloElMiniLogradoTraeHito();
    }

    @Test
    @DisplayName("Un mini-objetivo manual no tiene serie: no se incluye entre los medidos")
    void executeShouldLeaveOutTheManualMiniObjetivos() {
        givenFotos();
        givenMinis(miniObjetivo(), miniManual());

        whenSeObtieneElProgreso();

        thenSoloSeIncluyeElMiniMedido();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos devuelve la meta vacía")
    void executeShouldReturnAnEmptyGoalForAnObjetivoWithoutGoal() {
        givenUnObjetivoSinMeta();
        givenFotos();
        givenMinis();

        whenSeObtieneElProgreso();

        thenNoHayMetaNiHito();
    }

    @Test
    @DisplayName("Un objetivo alcanzado trae el hito de alcanzado")
    void executeShouldReturnTheAchievedMilestoneWhenTheObjetivoWasAchieved() {
        givenUnObjetivoAlcanzado();
        givenFotos();
        givenMinis();

        whenSeObtieneElProgreso();

        thenElHitoEsObjetivoAlcanzado();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no consulta sus fotos")
    void executeShouldFailWhenTheObjetivoBelongsToAnotherBusiness() {
        givenUnObjetivoDeOtroNegocio();

        whenSeObtieneElProgreso();

        thenRespondeComoInexistenteSinConsultarSusFotos();
    }

    // --- arrange ---
    private void givenUnNegocioConUnObjetivoConVentas() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor(almacen()));
    }

    private void givenUnNegocioSinVentas() {
        DatosDeProyecto sinVentas = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .build();
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor(sinVentas));
    }

    private void givenFotos(ProgresoObjetivo... fotos) {
        when(progresoObjetivoRepository.findAllByObjetivoId(OBJETIVO_ID)).thenReturn(List.of(fotos));
    }

    private void givenCierre(LocalDate semana, String resultado) {
        ResumenSemanal cierre = ResumenSemanal.de(objetivo(), semana, new BigDecimal(resultado),
                new ConteoDecisiones(0, 0, 0, 0), List.of(), "", List.of(), List.of(), AHORA);
        when(resumenSemanalRepository.findAllByObjetivoId(OBJETIVO_ID)).thenReturn(List.of(cierre));
    }

    private void givenMinis(MiniObjetivo... minis) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(minis));
    }

    private void givenUnObjetivoSinMeta() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivoSinMeta());
    }

    private void givenUnObjetivoAlcanzado() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(alcanzado);
    }

    private void givenUnObjetivoDeOtroNegocio() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- act ---
    private void whenSeObtieneElProgreso() {
        error = catchThrowable(() -> progreso =
                obtenerProgreso.execute(new ObtenerProgresoRequest(USUARIO_ID, OBJETIVO_ID)));
    }

    // --- assert ---
    private void thenLaMetaEsLaDelObjetivo() {
        assertThat(progreso.meta()).isEqualTo(META);
    }

    private void thenLaSerieMedidaTieneLaUltimaFotoDeEstaSemana() {
        assertThat(progreso.semanas()).filteredOn(punto -> !punto.estimado())
                .singleElement()
                .satisfies(punto -> {
                    assertThat(punto.semana()).isEqualTo(ESTA_SEMANA);
                    assertThat(punto.valor()).isEqualByComparingTo("-27000");
                });
    }

    private void thenLasSemanasAnterioresALaPrimeraFotoVanEstimadas() {
        assertThat(progreso.semanas()).hasSize(6);
        assertThat(progreso.semanas()).extracting(PuntoSemanal::estimado)
                .containsExactly(true, true, true, true, true, false);
        assertThat(progreso.semanas()).extracting(PuntoSemanal::semana).last().isEqualTo(ESTA_SEMANA);
        assertThat(progreso.semanas().get(4).semana()).isEqualTo(SEMANA_ANTERIOR);
    }

    private void thenLaSemanaAnteriorMuestraElResultadoDeSuCierre() {
        assertThat(progreso.semanas()).filteredOn(punto -> punto.semana().equals(SEMANA_ANTERIOR))
                .singleElement()
                .satisfies(punto -> {
                    assertThat(punto.valor()).isEqualByComparingTo("-31000");
                    assertThat(punto.estimado()).isFalse();
                });
    }

    private void thenTodasLasSemanasVanEstimadas() {
        assertThat(progreso.semanas()).hasSize(6).allMatch(PuntoSemanal::estimado);
    }

    private void thenLaSerieDelObjetivoEstaVacia() {
        assertThat(progreso.semanas()).isEmpty();
    }

    private void thenNoHaySeriesDeMinis() {
        assertThat(progreso.minis()).isEmpty();
    }

    private void thenSoloTraenSerieLosMinisEnCursoOLogrados() {
        assertThat(progreso.minis()).extracting(mini -> mini.mini().id()).containsExactly(MINI_ID, OTRO_MINI_ID);
    }

    private void thenCadaMiniTraeSuPropiaSerie() {
        assertThat(progreso.minis().get(0).semanas()).singleElement().satisfies(punto -> {
            assertThat(punto.valor()).isEqualByComparingTo("12000");
            assertThat(punto.estimado()).isFalse();
        });
        assertThat(progreso.minis().get(1).semanas()).singleElement()
                .satisfies(punto -> assertThat(punto.valor()).isEqualByComparingTo("14500"));
    }

    private void thenSoloElMiniLogradoTraeHito() {
        assertThat(progreso.minis().get(1).hito()).isEqualTo(HitoObjetivo.MINI_LOGRADO);
        assertThat(progreso.minis().get(0).hito()).isNull();
    }

    private void thenSoloSeIncluyeElMiniMedido() {
        assertThat(progreso.minis()).extracting(mini -> mini.mini().id()).containsExactly(MINI_ID);
    }

    private void thenNoHayMetaNiHito() {
        assertThat(progreso.meta()).isNull();
        assertThat(progreso.hito()).isNull();
    }

    private void thenElHitoEsObjetivoAlcanzado() {
        assertThat(progreso.hito()).isEqualTo(HitoObjetivo.OBJETIVO_ALCANZADO);
    }

    private void thenRespondeComoInexistenteSinConsultarSusFotos() {
        assertThat(error)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objetivo no encontrado");
        verifyNoInteractions(progresoObjetivoRepository, miniObjetivoRepository, propuestaMiniObjetivosService);
    }

    // --- helpers ---
    private static MiniObjetivo miniLogrado() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setId(OTRO_MINI_ID);
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        return logrado;
    }

    private static MiniObjetivo miniReemplazado() {
        MiniObjetivo reemplazado = miniObjetivo();
        reemplazado.setEstado(EstadoMiniObjetivo.REEMPLAZADO);
        reemplazado.setId(MiniObjetivoId.nuevo());
        return reemplazado;
    }

    private static ProgresoObjetivo foto(MiniObjetivoId miniObjetivoId, LocalDate dia, String valor) {
        return ProgresoObjetivo.de(OBJETIVO_ID, miniObjetivoId, dia, new BigDecimal(valor), AHORA);
    }

    private static MedidorMiniObjetivo medidor(DatosDeProyecto datos) {
        return MedidorMiniObjetivo.de(List.of(datos), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
    }
}
