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
    void unObjetivoNuevoTomaElNegocioLaMetaYLaFechaYTodaviaNoTieneId() {
        cuandoSeCreaUnObjetivoNuevo();

        entoncesElObjetivoNoTieneIdYCopiaLosDatos();
    }

    @Test
    @DisplayName("Un objetivo nuevo todavía no está alcanzado")
    void unObjetivoNuevoTodaviaNoEstaAlcanzado() {
        cuandoSeCreaUnObjetivoNuevo();

        entoncesElObjetivoNoEstaAlcanzado();
    }

    @Test
    @DisplayName("Al marcarlo alcanzado se anota cuándo")
    void alMarcarloAlcanzadoSeAnotaCuando() {
        dadoUnObjetivoNuevo();

        cuandoSeMarcaAlcanzado(AHORA.plusDays(1));

        entoncesElObjetivoEstaAlcanzadoDesde(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("No llega a la meta cuando el resultado es menor")
    void noLlegaALaMetaCuandoElResultadoEsMenor() {
        dadoUnObjetivoNuevo();

        cuandoSeConsultaSiLlegaA(META.subtract(BigDecimal.ONE));

        entoncesNoLlegaALaMeta();
    }

    @Test
    @DisplayName("Llega a la meta cuando el resultado es igual")
    void llegaALaMetaCuandoElResultadoEsIgual() {
        dadoUnObjetivoNuevo();

        cuandoSeConsultaSiLlegaA(META);

        entoncesLlegaALaMeta();
    }

    @Test
    @DisplayName("Llega a la meta cuando el resultado es mayor")
    void llegaALaMetaCuandoElResultadoEsMayor() {
        dadoUnObjetivoNuevo();

        cuandoSeConsultaSiLlegaA(META.add(BigDecimal.ONE));

        entoncesLlegaALaMeta();
    }

    @Test
    @DisplayName("Un objetivo que no fue alcanzado no tiene hito")
    void unObjetivoQueNoFueAlcanzadoNoTieneHito() {
        dadoUnObjetivoNuevo();

        cuandoSeConsultaElHito();

        entoncesNoTieneHito();
    }

    @Test
    @DisplayName("Un objetivo alcanzado tiene el hito de alcanzado")
    void unObjetivoAlcanzadoTieneElHitoDeAlcanzado() {
        dadoUnObjetivoNuevo();

        cuandoSeMarcaAlcanzado(AHORA);
        cuandoSeConsultaElHito();

        entoncesTieneElHitoDeAlcanzado();
    }

    @Test
    @DisplayName("Un objetivo nuevo guarda el texto sin espacios en los extremos")
    void unObjetivoNuevoGuardaElTextoSinEspaciosEnLosExtremos() {
        cuandoSeCreaUnObjetivoConTexto("  " + TEXTO + " \n");

        entoncesElTextoEs(TEXTO);
    }

    @Test
    @DisplayName("Un objetivo nuevo es monetario y no lo armó Sami")
    void unObjetivoNuevoEsMonetarioYNoLoArmoSami() {
        cuandoSeCreaUnObjetivoNuevo();

        entoncesElObjetivoEsMonetarioYNoLoArmoSami();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos nunca llega a la meta, por alto que sea el resultado")
    void unObjetivoSinMetaEnPesosNuncaLlegaALaMetaPorAltoQueSeaElResultado() {
        dadoUnObjetivoSinMeta();

        cuandoSeConsultaSiLlegaA(RESULTADO_ENORME);

        entoncesNoLlegaALaMeta();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami no tiene meta, trae su tipo y el texto sin espacios en los extremos")
    void unObjetivoArmadoConSamiNoTieneMetaTraeSuTipoYElTextoSinEspaciosEnLos() {
        cuandoSeArmaConSami(TipoObjetivo.HIBRIDO, true);

        entoncesElObjetivoDeSamiNoTieneMetaYTraeSuTipo();
    }

    @Test
    @DisplayName("El seguimiento manual no cuenta como armado con Sami")
    void elSeguimientoManualNoCuentaComoArmadoConSami() {
        cuandoSeArmaConSami(TipoObjetivo.SIN_DATOS, false);

        entoncesNoEstaArmadoConSami();
    }

    // --- arrange ---
    private void dadoUnObjetivoNuevo() {
        cuandoSeCreaUnObjetivoNuevo();
    }

    private void dadoUnObjetivoSinMeta() {
        objetivo = objetivoSinMeta();
    }

    // --- act ---
    private void cuandoSeCreaUnObjetivoNuevo() {
        objetivo = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);
    }

    private void cuandoSeCreaUnObjetivoConTexto(String texto) {
        objetivo = Objetivo.nuevo(NEGOCIO_ID, texto, META, AHORA);
    }

    private void cuandoSeMarcaAlcanzado(OffsetDateTime cuando) {
        objetivo.marcarAlcanzado(cuando);
    }

    private void cuandoSeConsultaSiLlegaA(BigDecimal resultado) {
        llegaALaMeta = objetivo.llegaA(resultado);
    }

    private void cuandoSeConsultaElHito() {
        hito = objetivo.hito();
    }

    private void cuandoSeArmaConSami(TipoObjetivo tipo, boolean armadoConSami) {
        objetivo = Objetivo.conSami(NEGOCIO_ID, "  " + TEXTO_SIN_META + " ", tipo, armadoConSami, AHORA);
    }

    // --- assert ---
    private void entoncesElObjetivoNoTieneIdYCopiaLosDatos() {
        assertThat(objetivo.getId()).isNull();
        assertThat(objetivo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(objetivo.getTexto()).isEqualTo(TEXTO);
        assertThat(objetivo.getMeta()).isEqualTo(META);
        assertThat(objetivo.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElObjetivoNoEstaAlcanzado() {
        assertThat(objetivo.estaAlcanzado()).isFalse();
        assertThat(objetivo.getAlcanzadoEn()).isNull();
    }

    private void entoncesElObjetivoEstaAlcanzadoDesde(OffsetDateTime cuando) {
        assertThat(objetivo.estaAlcanzado()).isTrue();
        assertThat(objetivo.getAlcanzadoEn()).isEqualTo(cuando);
    }

    private void entoncesLlegaALaMeta() {
        assertThat(llegaALaMeta).isTrue();
    }

    private void entoncesNoLlegaALaMeta() {
        assertThat(llegaALaMeta).isFalse();
    }

    private void entoncesNoTieneHito() {
        assertThat(hito).isEmpty();
    }

    private void entoncesTieneElHitoDeAlcanzado() {
        assertThat(hito).contains(HitoObjetivo.OBJETIVO_ALCANZADO);
    }

    private void entoncesElTextoEs(String esperado) {
        assertThat(objetivo.getTexto()).isEqualTo(esperado);
    }

    private void entoncesElObjetivoEsMonetarioYNoLoArmoSami() {
        assertThat(objetivo.getTipo()).isEqualTo(TipoObjetivo.MONETARIO);
        entoncesNoEstaArmadoConSami();
    }

    private void entoncesNoEstaArmadoConSami() {
        assertThat(objetivo.isArmadoConSami()).isFalse();
    }

    private void entoncesElObjetivoDeSamiNoTieneMetaYTraeSuTipo() {
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
