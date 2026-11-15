package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.MiniProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ObtenerProgresoResponseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
    private static final LocalDate DIA = LocalDate.of(2026, 11, 11);
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    @Test
    @DisplayName("Un punto semanal toma el valor de la foto y la semana de su día, como medido")
    void puntoSemanalDeShouldUseTheWeekOfThePhotoAsMeasured() {
        ProgresoObjetivo foto = ProgresoObjetivo.de(OBJETIVO_ID, null, DIA, BigDecimal.TEN, AHORA);

        PuntoSemanal result = PuntoSemanal.de(foto);

        assertThat(result.semana()).isEqualTo(SEMANA);
        assertThat(result.valor()).isEqualTo(BigDecimal.TEN);
        assertThat(result.estimado()).isFalse();
    }

    @Test
    @DisplayName("El progreso de un mini-objetivo trae su ficha, su hito y la última foto de cada semana")
    void miniProgresoDeShouldCopyTheMiniObjetivoAndItsSeries() {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(EstadoMiniObjetivo.LOGRADO);
        List<ProgresoObjetivo> fotos = List.of(
                ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, DIA.minusDays(1), BigDecimal.ONE, AHORA),
                ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, DIA, BigDecimal.TEN, AHORA));

        MiniProgreso result = MiniProgreso.de(mini, HOY, fotos);

        assertThat(result.mini().id()).isEqualTo(MINI_ID);
        assertThat(result.hito()).isEqualTo(HitoObjetivo.MINI_LOGRADO);
        assertThat(result.semanas()).singleElement()
                .satisfies(punto -> assertThat(punto.valor()).isEqualTo(BigDecimal.TEN));
    }

    @Test
    @DisplayName("Un mini-objetivo sin nada para destacar no trae hito")
    void miniProgresoDeShouldHaveNoMilestoneWhenThereIsNothingToHighlight() {
        MiniProgreso result = MiniProgreso.de(miniObjetivo(), HOY, List.of());

        assertThat(result.hito()).isNull();
        assertThat(result.semanas()).isEmpty();
    }

    @Test
    @DisplayName("La respuesta copia la meta y el hito del objetivo")
    void deShouldCopyTheGoalAndTheMilestone() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        List<PuntoSemanal> semanas = List.of(new PuntoSemanal(SEMANA, BigDecimal.TEN, true));

        ObtenerProgresoResponse result = ObtenerProgresoResponse.de(alcanzado, semanas, List.of());

        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.hito()).isEqualTo(HitoObjetivo.OBJETIVO_ALCANZADO);
        assertThat(result.semanas()).isEqualTo(semanas);
        assertThat(result.minis()).isEmpty();
    }

    @Test
    @DisplayName("Un objetivo que no llegó a la meta no trae hito")
    void deShouldHaveNoMilestoneWhenTheObjetivoWasNotAchieved() {
        ObtenerProgresoResponse result = ObtenerProgresoResponse.de(objetivo(), List.of(), List.of());

        assertThat(result.hito()).isNull();
    }
}
