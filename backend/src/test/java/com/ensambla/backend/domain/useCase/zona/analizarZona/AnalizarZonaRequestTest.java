package com.ensambla.backend.domain.useCase.zona.analizarZona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.ZonasDePrueba.LATITUD;
import static com.ensambla.backend.support.ZonasDePrueba.LONGITUD;
import static com.ensambla.backend.support.ZonasDePrueba.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class AnalizarZonaRequestTest {

    private AnalizarZonaRequest pedido;
    private boolean tienePunto;

    @Test
    @DisplayName("Tiene punto cuando vienen latitud y longitud")
    void tienePuntoCuandoVienenLatitudYLongitud() {
        dadoUnPedidoCon(LATITUD, LONGITUD);

        cuandoSePreguntaSiTienePunto();

        entoncesTienePunto();
    }

    @Test
    @DisplayName("No tiene punto cuando falta la latitud")
    void noTienePuntoCuandoFaltaLaLatitud() {
        dadoUnPedidoCon(null, LONGITUD);

        cuandoSePreguntaSiTienePunto();

        entoncesNoTienePunto();
    }

    @Test
    @DisplayName("No tiene punto cuando falta la longitud")
    void noTienePuntoCuandoFaltaLaLongitud() {
        dadoUnPedidoCon(LATITUD, null);

        cuandoSePreguntaSiTienePunto();

        entoncesNoTienePunto();
    }

    // --- dado ---
    private void dadoUnPedidoCon(BigDecimal latitud, BigDecimal longitud) {
        pedido = new AnalizarZonaRequest("San Justo", latitud, longitud, USUARIO_ID);
    }

    // --- cuando ---
    private void cuandoSePreguntaSiTienePunto() {
        tienePunto = pedido.tienePunto();
    }

    // --- entonces ---
    private void entoncesTienePunto() {
        assertThat(tienePunto).isTrue();
    }

    private void entoncesNoTienePunto() {
        assertThat(tienePunto).isFalse();
    }
}
