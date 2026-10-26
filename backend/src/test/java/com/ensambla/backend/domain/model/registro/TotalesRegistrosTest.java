package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.model.campo.CampoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class TotalesRegistrosTest {

    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Arma una suma por campo numérico en el orden recibido")
    void armarShouldKeepOneSumPerNumericCampoInOrder() {
        TotalesRegistros totales = TotalesRegistros.armar(
                7,
                List.of(PRECIO_ID, CANTIDAD_ID),
                Map.of(CANTIDAD_ID, new BigDecimal("12.5"), PRECIO_ID, new BigDecimal("300")));

        assertThat(totales.cantidad()).isEqualTo(7);
        assertThat(totales.sumas()).containsExactly(
                entry(PRECIO_ID, new BigDecimal("300")),
                entry(CANTIDAD_ID, new BigDecimal("12.5")));
    }

    @Test
    @DisplayName("Un campo numérico que ningún registro cargó suma cero")
    void armarShouldSumZeroWhenNoRegistroLoadedTheCampo() {
        TotalesRegistros totales = TotalesRegistros.armar(3, List.of(CANTIDAD_ID), Map.of());

        assertThat(totales.sumas()).containsOnly(entry(CANTIDAD_ID, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Ignora las sumas de campos que no son numéricos del bloque")
    void armarShouldIgnoreSumsOfOtherCampos() {
        TotalesRegistros totales = TotalesRegistros.armar(
                3, List.of(CANTIDAD_ID), Map.of(PRECIO_ID, BigDecimal.TEN));

        assertThat(totales.sumas()).containsOnlyKeys(CANTIDAD_ID);
    }

    @Test
    @DisplayName("Sin campos numéricos conserva la cantidad y no tiene sumas")
    void armarShouldKeepCountWhenThereAreNoNumericCampos() {
        TotalesRegistros totales = TotalesRegistros.armar(12, List.of(), Map.of());

        assertThat(totales.cantidad()).isEqualTo(12);
        assertThat(totales.sumas()).isEmpty();
    }
}
