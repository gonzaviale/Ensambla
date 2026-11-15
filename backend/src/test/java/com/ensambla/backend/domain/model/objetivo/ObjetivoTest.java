package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoTest {

    @Test
    @DisplayName("Un objetivo nuevo toma el negocio, la meta y la fecha, y todavía no tiene id")
    void nuevoShouldCopyEveryField() {
        Objetivo result = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTexto()).isEqualTo(TEXTO);
        assertThat(result.getMeta()).isEqualTo(META);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Un objetivo nuevo todavía no está alcanzado")
    void nuevoShouldNotBeReached() {
        Objetivo result = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        assertThat(result.estaAlcanzado()).isFalse();
        assertThat(result.getAlcanzadoEn()).isNull();
    }

    @Test
    @DisplayName("Al marcarlo alcanzado se anota cuándo")
    void marcarAlcanzadoShouldRecordWhen() {
        Objetivo objetivo = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        objetivo.marcarAlcanzado(AHORA.plusDays(1));

        assertThat(objetivo.estaAlcanzado()).isTrue();
        assertThat(objetivo.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("Llega a la meta cuando el resultado es igual o mayor")
    void llegaAShouldBeTrueWhenTheResultIsAtLeastTheGoal() {
        Objetivo objetivo = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        assertThat(objetivo.llegaA(META.subtract(BigDecimal.ONE))).isFalse();
        assertThat(objetivo.llegaA(META)).isTrue();
        assertThat(objetivo.llegaA(META.add(BigDecimal.ONE))).isTrue();
    }

    @Test
    @DisplayName("Un objetivo alcanzado tiene el hito de alcanzado; uno que no, no tiene hito")
    void hitoShouldBeAchievedOnlyWhenTheObjetivoWasAchieved() {
        Objetivo objetivo = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);
        assertThat(objetivo.hito()).isEmpty();

        objetivo.marcarAlcanzado(AHORA);

        assertThat(objetivo.hito()).contains(HitoObjetivo.OBJETIVO_ALCANZADO);
    }

    @Test
    @DisplayName("Un objetivo nuevo guarda el texto sin espacios en los extremos")
    void nuevoShouldStripText() {
        Objetivo result = Objetivo.nuevo(NEGOCIO_ID, "  " + TEXTO + " \n", META, AHORA);

        assertThat(result.getTexto()).isEqualTo(TEXTO);
    }
}
