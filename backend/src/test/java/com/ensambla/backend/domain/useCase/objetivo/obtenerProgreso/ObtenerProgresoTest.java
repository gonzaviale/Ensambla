package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
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
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.NegocioDePrueba.YERBA;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ObtenerProgreso obtenerProgreso;

    @BeforeEach
    void setUp() {
        obtenerProgreso = new ObtenerProgreso(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                progresoObjetivoRepository,
                propuestaMiniObjetivosService,
                CLOCK);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor(almacen()));
    }

    @Test
    @DisplayName("Devuelve la meta del objetivo y su serie semanal medida, con la última foto de cada semana")
    void executeShouldReturnTheMeasuredWeeklySeriesOfTheObjetivo() {
        givenFotos(foto(null, HOY.minusDays(1), "-30000"), foto(null, HOY, "-27000"));
        givenMinis();

        ObtenerProgresoResponse result = whenObtener();

        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.semanas()).filteredOn(punto -> !punto.estimado())
                .singleElement()
                .satisfies(punto -> {
                    assertThat(punto.semana()).isEqualTo(ESTA_SEMANA);
                    assertThat(punto.valor()).isEqualByComparingTo("-27000");
                });
    }

    @Test
    @DisplayName("Completa las semanas anteriores a la primera foto con el resultado calculado, marcadas como estimadas")
    void executeShouldCompleteThePreviousWeeksWithEstimates() {
        givenFotos(foto(null, HOY, "-27000"));
        givenMinis();

        ObtenerProgresoResponse result = whenObtener();

        assertThat(result.semanas()).hasSize(6);
        assertThat(result.semanas()).extracting(PuntoSemanal::estimado)
                .containsExactly(true, true, true, true, true, false);
        assertThat(result.semanas()).extracting(PuntoSemanal::semana).last().isEqualTo(ESTA_SEMANA);
        assertThat(result.semanas().get(4).semana()).isEqualTo(SEMANA_ANTERIOR);
    }

    @Test
    @DisplayName("Sin ninguna foto todo lo que se sabe del pasado va estimado")
    void executeShouldShowEverythingAsEstimatedWithoutPhotos() {
        givenFotos();
        givenMinis();

        ObtenerProgresoResponse result = whenObtener();

        assertThat(result.semanas()).hasSize(6).allMatch(PuntoSemanal::estimado);
    }

    @Test
    @DisplayName("Sin ventas ni fotos la serie del objetivo está vacía")
    void executeShouldReturnAnEmptySeriesWithoutSalesOrPhotos() {
        DatosDeProyecto sinVentas = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .build();
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor(sinVentas));
        givenFotos();
        givenMinis();

        ObtenerProgresoResponse result = whenObtener();

        assertThat(result.semanas()).isEmpty();
        assertThat(result.minis()).isEmpty();
    }

    @Test
    @DisplayName("Cada mini-objetivo en curso o logrado trae su propia serie y los reemplazados o quitados no")
    void executeShouldReturnTheSeriesOfTheMiniObjetivosInProgressOrAchieved() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setId(OTRO_MINI_ID);
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        MiniObjetivo reemplazado = miniObjetivo();
        reemplazado.setEstado(EstadoMiniObjetivo.REEMPLAZADO);
        reemplazado.setId(MiniObjetivoId.nuevo());
        givenFotos(foto(MINI_ID, HOY, "12000"), foto(OTRO_MINI_ID, HOY, "14500"), foto(null, HOY, "-27000"));
        givenMinis(miniObjetivo(), logrado, reemplazado);

        ObtenerProgresoResponse result = whenObtener();

        assertThat(result.minis()).extracting(mini -> mini.mini().id()).containsExactly(MINI_ID, OTRO_MINI_ID);
        assertThat(result.minis().get(0).semanas()).singleElement().satisfies(punto -> {
            assertThat(punto.valor()).isEqualByComparingTo("12000");
            assertThat(punto.estimado()).isFalse();
        });
        assertThat(result.minis().get(1).semanas()).singleElement()
                .satisfies(punto -> assertThat(punto.valor()).isEqualByComparingTo("14500"));
        assertThat(result.minis().get(1).hito()).isEqualTo(HitoObjetivo.MINI_LOGRADO);
        assertThat(result.minis().get(0).hito()).isNull();
    }

    @Test
    @DisplayName("Un objetivo alcanzado trae el hito de alcanzado")
    void executeShouldReturnTheAchievedMilestoneWhenTheObjetivoWasAchieved() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(alcanzado);
        givenFotos();
        givenMinis();

        ObtenerProgresoResponse result = whenObtener();

        assertThat(result.hito()).isEqualTo(HitoObjetivo.OBJETIVO_ALCANZADO);
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no consulta sus fotos")
    void executeShouldFailWhenTheObjetivoBelongsToAnotherBusiness() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(this::whenObtener)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objetivo no encontrado");
        verifyNoInteractions(progresoObjetivoRepository, miniObjetivoRepository, propuestaMiniObjetivosService);
    }

    // --- arrange ---
    private void givenFotos(ProgresoObjetivo... fotos) {
        when(progresoObjetivoRepository.findAllByObjetivoId(OBJETIVO_ID)).thenReturn(List.of(fotos));
    }

    private void givenMinis(MiniObjetivo... minis) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(minis));
    }

    // --- act ---
    private ObtenerProgresoResponse whenObtener() {
        return obtenerProgreso.execute(new ObtenerProgresoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    // --- helpers ---
    private static ProgresoObjetivo foto(
            MiniObjetivoId miniObjetivoId, LocalDate dia, String valor) {
        return ProgresoObjetivo.de(OBJETIVO_ID, miniObjetivoId, dia, new BigDecimal(valor), AHORA);
    }

    private static MedidorMiniObjetivo medidor(DatosDeProyecto datos) {
        return MedidorMiniObjetivo.de(List.of(datos), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
    }
}
