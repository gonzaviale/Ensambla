package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO_SIN_META;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoTest {

    private static final BigDecimal RESULTADO_ENORME = new BigDecimal("999999999");

    private Objetivo objetivo;
    private boolean llegaALaMeta;
    private Optional<HitoObjetivo> hito;

    @Test
    @DisplayName("Un objetivo nuevo toma el negocio, la meta y la fecha, y todavía no tiene id")
    void nuevoShouldCopyEveryField() {
        whenSeCreaUnObjetivoNuevo();

        thenElObjetivoNoTieneIdYCopiaLosDatos();
    }

    @Test
    @DisplayName("Un objetivo nuevo todavía no está alcanzado")
    void nuevoShouldNotBeReached() {
        whenSeCreaUnObjetivoNuevo();

        thenElObjetivoNoEstaAlcanzado();
    }

    @Test
    @DisplayName("Al marcarlo alcanzado se anota cuándo")
    void marcarAlcanzadoShouldRecordWhen() {
        givenUnObjetivoNuevo();

        whenSeMarcaAlcanzado(AHORA.plusDays(1));

        thenElObjetivoEstaAlcanzadoDesde(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("No llega a la meta cuando el resultado es menor")
    void llegaAShouldBeFalseWhenTheResultIsBelowTheGoal() {
        givenUnObjetivoNuevo();

        whenSeConsultaSiLlegaA(META.subtract(BigDecimal.ONE));

        thenNoLlegaALaMeta();
    }

    @Test
    @DisplayName("Llega a la meta cuando el resultado es igual")
    void llegaAShouldBeTrueWhenTheResultIsTheGoal() {
        givenUnObjetivoNuevo();

        whenSeConsultaSiLlegaA(META);

        thenLlegaALaMeta();
    }

    @Test
    @DisplayName("Llega a la meta cuando el resultado es mayor")
    void llegaAShouldBeTrueWhenTheResultIsAboveTheGoal() {
        givenUnObjetivoNuevo();

        whenSeConsultaSiLlegaA(META.add(BigDecimal.ONE));

        thenLlegaALaMeta();
    }

    @Test
    @DisplayName("Un objetivo que no fue alcanzado no tiene hito")
    void hitoShouldBeEmptyWhenTheObjetivoWasNotAchieved() {
        givenUnObjetivoNuevo();

        whenSeConsultaElHito();

        thenNoTieneHito();
    }

    @Test
    @DisplayName("Un objetivo alcanzado tiene el hito de alcanzado")
    void hitoShouldBeAchievedWhenTheObjetivoWasAchieved() {
        givenUnObjetivoNuevo();

        whenSeMarcaAlcanzado(AHORA);
        whenSeConsultaElHito();

        thenTieneElHitoDeAlcanzado();
    }

    @Test
    @DisplayName("Un objetivo nuevo guarda el texto sin espacios en los extremos")
    void nuevoShouldStripText() {
        whenSeCreaUnObjetivoConTexto("  " + TEXTO + " \n");

        thenElTextoEs(TEXTO);
    }

    @Test
    @DisplayName("Un objetivo nuevo es monetario y no lo armó Sami")
    void nuevoShouldBeMonetarioAndNotBuiltWithSami() {
        whenSeCreaUnObjetivoNuevo();

        thenElObjetivoEsMonetarioYNoLoArmoSami();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos nunca llega a la meta, por alto que sea el resultado")
    void llegaAShouldBeFalseWithoutGoal() {
        givenUnObjetivoSinMeta();

        whenSeConsultaSiLlegaA(RESULTADO_ENORME);

        thenNoLlegaALaMeta();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami no tiene meta, trae su tipo y el texto sin espacios en los extremos")
    void conSamiShouldBuildAnObjetivoWithoutGoal() {
        whenSeArmaConSami(TipoObjetivo.HIBRIDO, true);

        thenElObjetivoDeSamiNoTieneMetaYTraeSuTipo();
    }

    @Test
    @DisplayName("El seguimiento manual no cuenta como armado con Sami")
    void conSamiShouldKeepTheFlagThatSamiDidNotBuildIt() {
        whenSeArmaConSami(TipoObjetivo.SIN_DATOS, false);

        thenNoEstaArmadoConSami();
    }

    // --- arrange ---
    private void givenUnObjetivoNuevo() {
        whenSeCreaUnObjetivoNuevo();
    }

    private void givenUnObjetivoSinMeta() {
        objetivo = objetivoSinMeta();
    }

    // --- act ---
    private void whenSeCreaUnObjetivoNuevo() {
        objetivo = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);
    }

    private void whenSeCreaUnObjetivoConTexto(String texto) {
        objetivo = Objetivo.nuevo(NEGOCIO_ID, texto, META, AHORA);
    }

    private void whenSeMarcaAlcanzado(OffsetDateTime cuando) {
        objetivo.marcarAlcanzado(cuando);
    }

    private void whenSeConsultaSiLlegaA(BigDecimal resultado) {
        llegaALaMeta = objetivo.llegaA(resultado);
    }

    private void whenSeConsultaElHito() {
        hito = objetivo.hito();
    }

    private void whenSeArmaConSami(TipoObjetivo tipo, boolean armadoConSami) {
        objetivo = Objetivo.conSami(NEGOCIO_ID, "  " + TEXTO_SIN_META + " ", tipo, armadoConSami, AHORA);
    }

    // --- assert ---
    private void thenElObjetivoNoTieneIdYCopiaLosDatos() {
        assertThat(objetivo.getId()).isNull();
        assertThat(objetivo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(objetivo.getTexto()).isEqualTo(TEXTO);
        assertThat(objetivo.getMeta()).isEqualTo(META);
        assertThat(objetivo.getCreadoEn()).isEqualTo(AHORA);
    }

    private void thenElObjetivoNoEstaAlcanzado() {
        assertThat(objetivo.estaAlcanzado()).isFalse();
        assertThat(objetivo.getAlcanzadoEn()).isNull();
    }

    private void thenElObjetivoEstaAlcanzadoDesde(OffsetDateTime cuando) {
        assertThat(objetivo.estaAlcanzado()).isTrue();
        assertThat(objetivo.getAlcanzadoEn()).isEqualTo(cuando);
    }

    private void thenLlegaALaMeta() {
        assertThat(llegaALaMeta).isTrue();
    }

    private void thenNoLlegaALaMeta() {
        assertThat(llegaALaMeta).isFalse();
    }

    private void thenNoTieneHito() {
        assertThat(hito).isEmpty();
    }

    private void thenTieneElHitoDeAlcanzado() {
        assertThat(hito).contains(HitoObjetivo.OBJETIVO_ALCANZADO);
    }

    private void thenElTextoEs(String esperado) {
        assertThat(objetivo.getTexto()).isEqualTo(esperado);
    }

    private void thenElObjetivoEsMonetarioYNoLoArmoSami() {
        assertThat(objetivo.getTipo()).isEqualTo(TipoObjetivo.MONETARIO);
        thenNoEstaArmadoConSami();
    }

    private void thenNoEstaArmadoConSami() {
        assertThat(objetivo.isArmadoConSami()).isFalse();
    }

    private void thenElObjetivoDeSamiNoTieneMetaYTraeSuTipo() {
        assertThat(objetivo.getId()).isNull();
        assertThat(objetivo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(objetivo.getTexto()).isEqualTo(TEXTO_SIN_META);
        assertThat(objetivo.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(objetivo.getMeta()).isNull();
        assertThat(objetivo.isArmadoConSami()).isTrue();
        assertThat(objetivo.getCreadoEn()).isEqualTo(AHORA);
        assertThat(objetivo.estaAlcanzado()).isFalse();
    }
}
