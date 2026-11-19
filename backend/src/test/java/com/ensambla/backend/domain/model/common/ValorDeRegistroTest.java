package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ValorDeRegistroTest {

    private ValorDeRegistro valor;

    @Test
    @DisplayName("Un número cargado se lee como número")
    void unNumeroCargadoSeLeeComoNumero() {
        dadoUnValorCargado(12.5);

        entoncesSeLeeElNumero(new BigDecimal("12.5"));
    }

    @Test
    @DisplayName("Un texto no se lee como número")
    void unTextoNoSeLeeComoNumero() {
        dadoUnValorCargado("12");

        entoncesNoSeLeeNingunNumero();
    }

    @Test
    @DisplayName("Un número positivo se lee como número positivo")
    void unNumeroPositivoSeLeeComoNumeroPositivo() {
        dadoUnValorCargado(3);

        entoncesSeLeeElNumeroPositivo(new BigDecimal("3"));
    }

    @Test
    @DisplayName("Un cero no cuenta como número positivo")
    void unCeroNoCuentaComoNumeroPositivo() {
        dadoUnValorCargado(0);

        entoncesNoSeLeeNingunNumeroPositivo();
    }

    @Test
    @DisplayName("Un dato que falta no se lee como número positivo")
    void unDatoQueFaltaNoSeLeeComoNumeroPositivo() {
        dadoUnValorQueFalta();

        entoncesNoSeLeeNingunNumeroPositivo();
    }

    @Test
    @DisplayName("Un texto se lee sin los espacios de los bordes")
    void unTextoSeLeeSinLosEspaciosDeLosBordes() {
        dadoUnValorCargado("  Yerba ");

        entoncesSeLeeElTexto("Yerba");
    }

    @Test
    @DisplayName("Un texto en blanco no cuenta como texto")
    void unTextoEnBlancoNoCuentaComoTexto() {
        dadoUnValorCargado("   ");

        entoncesNoSeLeeNingunTexto();
    }

    @Test
    @DisplayName("Un número no cuenta como texto")
    void unNumeroNoCuentaComoTexto() {
        dadoUnValorCargado(7);

        entoncesNoSeLeeNingunTexto();
    }

    @Test
    @DisplayName("Sin texto se usa el texto de reemplazo")
    void sinTextoSeUsaElTextoDeReemplazo() {
        dadoUnValorQueFalta();

        entoncesSinTextoSeLee("Un producto sin nombre", "Un producto sin nombre");
    }

    @Test
    @DisplayName("Con texto no se usa el texto de reemplazo")
    void conTextoNoSeUsaElTextoDeReemplazo() {
        dadoUnValorCargado(" Yerba ");

        entoncesSinTextoSeLee("Un producto sin nombre", "Yerba");
    }

    @Test
    @DisplayName("Una fecha ISO se lee como fecha")
    void unaFechaIsoSeLeeComoFecha() {
        dadoUnValorCargado(" 2026-11-18 ");

        entoncesSeLeeLaFecha(LocalDate.of(2026, 11, 18));
    }

    @Test
    @DisplayName("Un texto que no es fecha no se lee como fecha")
    void unTextoQueNoEsFechaNoSeLeeComoFecha() {
        dadoUnValorCargado("ayer");

        entoncesNoSeLeeNingunaFecha();
    }

    @Test
    @DisplayName("Un dato que falta no se lee como fecha")
    void unDatoQueFaltaNoSeLeeComoFecha() {
        dadoUnValorQueFalta();

        entoncesNoSeLeeNingunaFecha();
    }

    private void dadoUnValorCargado(Object cargado) {
        valor = ValorDeRegistro.de(cargado);
    }

    private void dadoUnValorQueFalta() {
        valor = ValorDeRegistro.de(null);
    }

    private void entoncesSeLeeElNumero(BigDecimal numero) {
        assertThat(valor.comoNumero()).isEqualByComparingTo(numero);
    }

    private void entoncesNoSeLeeNingunNumero() {
        assertThat(valor.comoNumero()).isNull();
    }

    private void entoncesSeLeeElNumeroPositivo(BigDecimal numero) {
        assertThat(valor.comoNumeroPositivo()).isEqualByComparingTo(numero);
    }

    private void entoncesNoSeLeeNingunNumeroPositivo() {
        assertThat(valor.comoNumeroPositivo()).isNull();
    }

    private void entoncesSeLeeElTexto(String texto) {
        assertThat(valor.comoTexto()).isEqualTo(texto);
    }

    private void entoncesNoSeLeeNingunTexto() {
        assertThat(valor.comoTexto()).isNull();
    }

    private void entoncesSinTextoSeLee(String reemplazo, String esperado) {
        assertThat(valor.comoTextoO(reemplazo)).isEqualTo(esperado);
    }

    private void entoncesSeLeeLaFecha(LocalDate fecha) {
        assertThat(valor.comoFecha()).isEqualTo(fecha);
    }

    private void entoncesNoSeLeeNingunaFecha() {
        assertThat(valor.comoFecha()).isNull();
    }
}
