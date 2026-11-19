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

    private TotalesRegistros totales;

    @Test
    @DisplayName("Arma una suma por campo numérico en el orden recibido")
    void armaUnaSumaPorCampoNumericoEnElOrdenRecibido() {
        cuandoSeArmanLosTotales(7, List.of(PRECIO_ID, CANTIDAD_ID),
                Map.of(CANTIDAD_ID, new BigDecimal("12.5"), PRECIO_ID, new BigDecimal("300")));

        entoncesLaCantidadEs(7);
        entoncesLasSumasSonEnOrden(entry(PRECIO_ID, new BigDecimal("300")), entry(CANTIDAD_ID, new BigDecimal("12.5")));
    }

    @Test
    @DisplayName("Un campo numérico que ningún registro cargó suma cero")
    void unCampoNumericoQueNingunRegistroCargoSumaCero() {
        cuandoSeArmanLosTotales(3, List.of(CANTIDAD_ID), Map.of());

        entoncesLasSumasSonEnOrden(entry(CANTIDAD_ID, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Ignora las sumas de campos que no son numéricos del bloque")
    void ignoraLasSumasDeCamposQueNoSonNumericosDelBloque() {
        cuandoSeArmanLosTotales(3, List.of(CANTIDAD_ID), Map.of(PRECIO_ID, BigDecimal.TEN));

        entoncesSoloHaySumaDe(CANTIDAD_ID);
    }

    @Test
    @DisplayName("Sin campos numéricos conserva la cantidad y no tiene sumas")
    void sinCamposNumericosConservaLaCantidadYNoTieneSumas() {
        cuandoSeArmanLosTotales(12, List.of(), Map.of());

        entoncesLaCantidadEs(12);
        entoncesNoHaySumas();
    }

    // --- cuando ---
    private void cuandoSeArmanLosTotales(long cantidad, List<CampoId> numericos, Map<CampoId, BigDecimal> sumas) {
        totales = TotalesRegistros.armar(cantidad, numericos, sumas);
    }

    // --- entonces ---
    private void entoncesLaCantidadEs(long esperada) {
        assertThat(totales.cantidad()).isEqualTo(esperada);
    }

    @SafeVarargs
    private void entoncesLasSumasSonEnOrden(Map.Entry<CampoId, BigDecimal>... esperadas) {
        assertThat(totales.sumas()).containsExactly(esperadas);
    }

    private void entoncesSoloHaySumaDe(CampoId campoId) {
        assertThat(totales.sumas()).containsOnlyKeys(campoId);
    }

    private void entoncesNoHaySumas() {
        assertThat(totales.sumas()).isEmpty();
    }
}
