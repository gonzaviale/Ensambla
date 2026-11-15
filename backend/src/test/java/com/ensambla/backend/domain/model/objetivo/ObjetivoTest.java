package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO_SIN_META;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
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

    @Test
    @DisplayName("Un objetivo nuevo es monetario y no lo armó Sami")
    void nuevoShouldBeMonetarioAndNotBuiltWithSami() {
        Objetivo result = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoObjetivo.MONETARIO);
        assertThat(result.isArmadoConSami()).isFalse();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos nunca llega a la meta, por alto que sea el resultado")
    void llegaAShouldBeFalseWithoutGoal() {
        Objetivo objetivo = objetivoSinMeta();

        assertThat(objetivo.llegaA(new BigDecimal("999999999"))).isFalse();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami no tiene meta, trae su tipo y el texto sin espacios en los extremos")
    void conSamiShouldBuildAnObjetivoWithoutGoal() {
        Objetivo result = Objetivo.conSami(NEGOCIO_ID, "  " + TEXTO_SIN_META + " ", TipoObjetivo.HIBRIDO, true, AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTexto()).isEqualTo(TEXTO_SIN_META);
        assertThat(result.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(result.getMeta()).isNull();
        assertThat(result.isArmadoConSami()).isTrue();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.estaAlcanzado()).isFalse();
    }

    @Test
    @DisplayName("El seguimiento manual no cuenta como armado con Sami")
    void conSamiShouldKeepTheFlagThatSamiDidNotBuildIt() {
        Objetivo result = Objetivo.conSami(NEGOCIO_ID, TEXTO_SIN_META, TipoObjetivo.SIN_DATOS, false, AHORA);

        assertThat(result.isArmadoConSami()).isFalse();
    }
}
