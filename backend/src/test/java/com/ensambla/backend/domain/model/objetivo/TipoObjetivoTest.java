package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static org.assertj.core.api.Assertions.assertThat;

class TipoObjetivoTest {

    private static final MiniObjetivoPropuesto MEDIDO = MiniObjetivoPropuesto.medido(candidato());
    private static final MiniObjetivoPropuesto MANUAL =
            MiniObjetivoPropuesto.manual(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION), true);

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
        assertThat(TipoObjetivo.de(List.of(MEDIDO, MEDIDO))).isEqualTo(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo mini-objetivos manuales el objetivo es sin datos")
    void deShouldBeSinDatosWhenAllAreManual() {
        assertThat(TipoObjetivo.de(List.of(MANUAL))).isEqualTo(TipoObjetivo.SIN_DATOS);
    }

    @Test
    @DisplayName("Con mini-objetivos medidos y manuales el objetivo es híbrido")
    void deShouldBeHibridoWhenMixed() {
        assertThat(TipoObjetivo.de(List.of(MEDIDO, MANUAL))).isEqualTo(TipoObjetivo.HIBRIDO);
    }
}
