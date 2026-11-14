package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static org.assertj.core.api.Assertions.assertThat;

class TipoObjetivoTest {

    @Test
    @DisplayName("Sólo el monetario tiene meta en pesos")
    void tieneMetaShouldBeTrueOnlyForMonetario() {
        assertThat(TipoObjetivo.MONETARIO.tieneMeta()).isTrue();
        assertThat(TipoObjetivo.CON_DATOS.tieneMeta()).isFalse();
        assertThat(TipoObjetivo.SIN_DATOS.tieneMeta()).isFalse();
        assertThat(TipoObjetivo.HIBRIDO.tieneMeta()).isFalse();
    }

    @Test
    @DisplayName("Con sólo mini-objetivos medidos el objetivo es con datos")
    void deShouldBeConDatosWhenAllAreMeasured() {
        TipoObjetivo result = TipoObjetivo.de(List.of(miniObjetivo()));

        assertThat(result).isEqualTo(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo mini-objetivos manuales el objetivo es sin datos")
    void deShouldBeSinDatosWhenAllAreManual() {
        TipoObjetivo result = TipoObjetivo.de(List.of(miniManual()));

        assertThat(result).isEqualTo(TipoObjetivo.SIN_DATOS);
    }

    @Test
    @DisplayName("Con mini-objetivos medidos y manuales el objetivo es híbrido")
    void deShouldBeHibridoWhenMixed() {
        TipoObjetivo result = TipoObjetivo.de(List.of(miniObjetivo(), miniManual()));

        assertThat(result).isEqualTo(TipoObjetivo.HIBRIDO);
    }
}
