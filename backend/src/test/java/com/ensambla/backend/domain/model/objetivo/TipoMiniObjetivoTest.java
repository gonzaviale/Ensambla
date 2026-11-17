package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TipoMiniObjetivoTest {

    private TipoMiniObjetivo tipo;
    private SentidoMiniObjetivo sentido;
    private boolean apuntaAUnProducto;

    @ParameterizedTest
    @CsvSource({
            "SUBIR_PRECIO, SUBE",
            "MOVER_PARADO, SUBE",
            "PONER_A_LA_VISTA, SUBE",
            "RECORTAR_GASTO, BAJA",
            "MEJORAR_MARGEN, SUBE",
            "VENTAS_SEMANALES, SUBE",
            "PRODUCTOS_BAJO_EL_MINIMO, BAJA",
            "CAPITAL_PARADO, BAJA",
            "VARIEDAD_VENDIDA, SUBE"})
    @DisplayName("Cada tipo sabe hacia dónde va su métrica")
    void sentidoShouldMatchEachTipo(TipoMiniObjetivo tipoDelMini, SentidoMiniObjetivo esperado) {
        givenUnTipo(tipoDelMini);

        whenSePreguntaHaciaDondeVa();

        thenSuMetricaVa(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "SUBIR_PRECIO, true",
            "MOVER_PARADO, true",
            "PONER_A_LA_VISTA, true",
            "RECORTAR_GASTO, false",
            "MEJORAR_MARGEN, false",
            "VENTAS_SEMANALES, false",
            "PRODUCTOS_BAJO_EL_MINIMO, false",
            "CAPITAL_PARADO, false",
            "VARIEDAD_VENDIDA, false"})
    @DisplayName("Cada tipo sabe si apunta a un producto")
    void esDeProductoShouldMatchEachTipo(TipoMiniObjetivo tipoDelMini, boolean esperado) {
        givenUnTipo(tipoDelMini);

        whenSePreguntaSiApuntaAUnProducto();

        thenApuntaAUnProducto(esperado);
    }

    // --- arrange ---
    private void givenUnTipo(TipoMiniObjetivo tipoDelMini) {
        tipo = tipoDelMini;
    }

    // --- act ---
    private void whenSePreguntaHaciaDondeVa() {
        sentido = tipo.sentido();
    }

    private void whenSePreguntaSiApuntaAUnProducto() {
        apuntaAUnProducto = tipo.esDeProducto();
    }

    // --- assert ---
    private void thenSuMetricaVa(SentidoMiniObjetivo esperado) {
        assertThat(sentido).isEqualTo(esperado);
    }

    private void thenApuntaAUnProducto(boolean esperado) {
        assertThat(apuntaAUnProducto).isEqualTo(esperado);
    }
}
