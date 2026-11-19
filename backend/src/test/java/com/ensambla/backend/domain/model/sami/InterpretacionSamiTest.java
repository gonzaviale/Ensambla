package com.ensambla.backend.domain.model.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretacionSamiTest {

    private static final String SIN_PLANTILLA = null;

    @Test
    @DisplayName("Alcanza cuando hay plantilla y la confianza llega al umbral")
    void alcanzaCuandoLaConfianzaLlegaAlUmbral() {
        entoncesAlcanzaElUmbral(unaInterpretacion("Ventas", 0.7), 0.7, true);
    }

    @Test
    @DisplayName("No alcanza cuando la confianza queda debajo del umbral")
    void noAlcanzaCuandoLaConfianzaQuedaDebajo() {
        entoncesAlcanzaElUmbral(unaInterpretacion("Ventas", 0.69), 0.7, false);
    }

    @Test
    @DisplayName("No alcanza si no hay plantilla, aunque el umbral sea cero")
    void noAlcanzaSinPlantilla() {
        entoncesAlcanzaElUmbral(unaInterpretacion(SIN_PLANTILLA, 0.0), 0.0, false);
    }

    private static InterpretacionSami unaInterpretacion(String plantilla, double confianza) {
        return new InterpretacionSami(null, plantilla, List.of(), confianza, List.of());
    }

    private static void entoncesAlcanzaElUmbral(InterpretacionSami interpretacion, double umbral, boolean alcanza) {
        assertThat(interpretacion.alcanza(umbral)).isEqualTo(alcanza);
    }
}