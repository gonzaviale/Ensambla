package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.CamposDePrueba.unCampoDeMoneda;
import static com.ensambla.backend.support.CamposDePrueba.unCampoDeOpcion;
import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTexto;
import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTipo;
import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static org.assertj.core.api.Assertions.assertThat;

class CampoEnUnReporteTest {

    private Campo campo;

    @Test
    @DisplayName("Un reporte se puede filtrar por una columna de opción")
    void unReporteSePuedeFiltrarPorUnaColumnaDeOpcion() {
        dadoUnaColumnaDeOpcionConLosEstadosPagadoYPendiente();

        entoncesAdmiteFiltrarPorValor();
    }

    @Test
    @DisplayName("Una columna de opción sólo admite filtrar por una de sus opciones")
    void unaColumnaDeOpcionSoloAdmiteFiltrarPorUnaDeSusOpciones() {
        dadoUnaColumnaDeOpcionConLosEstadosPagadoYPendiente();

        entoncesAdmiteElValor("Pagado");
        entoncesNoAdmiteElValor("Cancelado");
    }

    @Test
    @DisplayName("Un reporte se puede filtrar por una columna de sí o no")
    void unReporteSePuedeFiltrarPorUnaColumnaDeSiONo() {
        dadoUnaColumnaDeSiONo();

        entoncesAdmiteFiltrarPorValor();
    }

    @Test
    @DisplayName("Una columna de sí o no sólo admite filtrar por verdadero o falso")
    void unaColumnaDeSiONoSoloAdmiteFiltrarPorVerdaderoOFalso() {
        dadoUnaColumnaDeSiONo();

        entoncesAdmiteElValor("true");
        entoncesAdmiteElValor("false");
        entoncesNoAdmiteElValor("tal vez");
    }

    @Test
    @DisplayName("Un reporte no se filtra por el valor de una columna de texto")
    void unReporteNoSeFiltraPorElValorDeUnaColumnaDeTexto() {
        dadoUnaColumnaDeTexto();

        entoncesNoAdmiteFiltrarPorValor();
        entoncesNoAdmiteElValor("Marta");
    }

    @Test
    @DisplayName("La fila de totales del reporte suma las columnas de número")
    void laFilaDeTotalesDelReporteSumaLasColumnasDeNumero() {
        dadoUnaColumnaNumerica();

        entoncesSeSumaEnElReporte();
    }

    @Test
    @DisplayName("La fila de totales del reporte no suma las columnas de moneda ni de texto")
    void laFilaDeTotalesDelReporteNoSumaLasColumnasDeMonedaNiDeTexto() {
        dadoUnaColumnaDeMoneda();
        entoncesNoSeSumaEnElReporte();

        dadoUnaColumnaDeTexto();
        entoncesNoSeSumaEnElReporte();
    }

    // --- dado ---
    private void dadoUnaColumnaDeOpcionConLosEstadosPagadoYPendiente() {
        campo = unCampoDeOpcion("Estado", List.of("Pagado", "Pendiente"));
    }

    private void dadoUnaColumnaDeSiONo() {
        campo = unCampoDeTipo(TipoCampo.SI_NO);
    }

    private void dadoUnaColumnaDeTexto() {
        campo = unCampoDeTexto("Nombre");
    }

    private void dadoUnaColumnaNumerica() {
        campo = unCampoNumerico("Cantidad");
    }

    private void dadoUnaColumnaDeMoneda() {
        campo = unCampoDeMoneda("Precio");
    }

    // --- entonces ---
    private void entoncesAdmiteFiltrarPorValor() {
        assertThat(campo.admiteFiltroPorValor()).isTrue();
    }

    private void entoncesNoAdmiteFiltrarPorValor() {
        assertThat(campo.admiteFiltroPorValor()).isFalse();
    }

    private void entoncesAdmiteElValor(String valor) {
        assertThat(campo.admiteFiltrarPor(valor)).isTrue();
    }

    private void entoncesNoAdmiteElValor(String valor) {
        assertThat(campo.admiteFiltrarPor(valor)).isFalse();
    }

    private void entoncesSeSumaEnElReporte() {
        assertThat(campo.sumaEnElReporte()).isTrue();
    }

    private void entoncesNoSeSumaEnElReporte() {
        assertThat(campo.sumaEnElReporte()).isFalse();
    }
}
