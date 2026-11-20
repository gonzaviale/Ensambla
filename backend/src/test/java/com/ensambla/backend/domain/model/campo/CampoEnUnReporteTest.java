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

    @Test
    @DisplayName("Un reporte se puede filtrar por una columna de opción")
    void unReporteSePuedeFiltrarPorUnaColumnaDeOpcion() {
        Campo estado = unCampoDeOpcion("Estado", List.of("Pagado", "Pendiente"));

        entoncesAdmiteFiltrarPorValor(estado);
    }

    @Test
    @DisplayName("Una columna de opción sólo admite filtrar por una de sus opciones")
    void unaColumnaDeOpcionSoloAdmiteFiltrarPorUnaDeSusOpciones() {
        Campo estado = unCampoDeOpcion("Estado", List.of("Pagado", "Pendiente"));

        entoncesAdmiteElValor(estado, "Pagado");
        entoncesNoAdmiteElValor(estado, "Cancelado");
    }

    @Test
    @DisplayName("Un reporte se puede filtrar por una columna de sí o no")
    void unReporteSePuedeFiltrarPorUnaColumnaDeSiONo() {
        Campo entregado = unCampoDeTipo(TipoCampo.SI_NO);

        entoncesAdmiteFiltrarPorValor(entregado);
    }

    @Test
    @DisplayName("Una columna de sí o no sólo admite filtrar por verdadero o falso")
    void unaColumnaDeSiONoSoloAdmiteFiltrarPorVerdaderoOFalso() {
        Campo entregado = unCampoDeTipo(TipoCampo.SI_NO);

        entoncesAdmiteElValor(entregado, "true");
        entoncesAdmiteElValor(entregado, "false");
        entoncesNoAdmiteElValor(entregado, "tal vez");
    }

    @Test
    @DisplayName("Un reporte no se filtra por el valor de una columna de texto")
    void unReporteNoSeFiltraPorElValorDeUnaColumnaDeTexto() {
        Campo nombre = unCampoDeTexto("Nombre");

        entoncesNoAdmiteFiltrarPorValor(nombre);
        entoncesNoAdmiteElValor(nombre, "Marta");
    }

    @Test
    @DisplayName("La fila de totales del reporte suma las columnas de número")
    void laFilaDeTotalesDelReporteSumaLasColumnasDeNumero() {
        Campo cantidad = unCampoNumerico("Cantidad");

        entoncesSeSumaEnElReporte(cantidad);
    }

    @Test
    @DisplayName("La fila de totales del reporte no suma las columnas de moneda ni de texto")
    void laFilaDeTotalesDelReporteNoSumaLasColumnasDeMonedaNiDeTexto() {
        entoncesNoSeSumaEnElReporte(unCampoDeMoneda("Precio"));
        entoncesNoSeSumaEnElReporte(unCampoDeTexto("Nombre"));
    }

    // --- entonces ---
    private void entoncesAdmiteFiltrarPorValor(Campo campo) {
        assertThat(campo.admiteFiltroPorValor()).isTrue();
    }

    private void entoncesNoAdmiteFiltrarPorValor(Campo campo) {
        assertThat(campo.admiteFiltroPorValor()).isFalse();
    }

    private void entoncesAdmiteElValor(Campo campo, String valor) {
        assertThat(campo.admiteFiltrarPor(valor)).isTrue();
    }

    private void entoncesNoAdmiteElValor(Campo campo, String valor) {
        assertThat(campo.admiteFiltrarPor(valor)).isFalse();
    }

    private void entoncesSeSumaEnElReporte(Campo campo) {
        assertThat(campo.sumaEnElReporte()).isTrue();
    }

    private void entoncesNoSeSumaEnElReporte(Campo campo) {
        assertThat(campo.sumaEnElReporte()).isFalse();
    }
}
