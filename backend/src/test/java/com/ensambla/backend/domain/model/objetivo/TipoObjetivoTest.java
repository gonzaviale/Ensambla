package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static org.assertj.core.api.Assertions.assertThat;

class TipoObjetivoTest {

    private static final MiniObjetivoPropuesto MEDIDO = MiniObjetivoPropuesto.medido(candidato());
    private static final MiniObjetivoPropuesto MANUAL =
            MiniObjetivoPropuesto.manual(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION), true);

    private boolean tieneMeta;
    private TipoObjetivo tipo;

    @ParameterizedTest(name = "{0}")
    @DisplayName("Sólo el monetario tiene meta en pesos")
    @CsvSource({"MONETARIO, true", "CON_DATOS, false", "SIN_DATOS, false", "HIBRIDO, false"})
    void tieneMetaShouldBeTrueOnlyForMonetario(TipoObjetivo tipoDado, boolean esperado) {
        whenSePreguntaSiTieneMeta(tipoDado);

        thenTieneMeta(esperado);
    }

    @Test
    @DisplayName("Con sólo mini-objetivos medidos el objetivo es con datos")
    void deShouldBeConDatosWhenAllAreMeasured() {
        whenSeDeduceElTipoDe(MEDIDO, MEDIDO);

        thenElTipoEs(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo mini-objetivos manuales el objetivo es sin datos")
    void deShouldBeSinDatosWhenAllAreManual() {
        whenSeDeduceElTipoDe(MANUAL);

        thenElTipoEs(TipoObjetivo.SIN_DATOS);
    }

    @Test
    @DisplayName("Con mini-objetivos medidos y manuales el objetivo es híbrido")
    void deShouldBeHibridoWhenMixed() {
        whenSeDeduceElTipoDe(MEDIDO, MANUAL);

        thenElTipoEs(TipoObjetivo.HIBRIDO);
    }

    // --- act ---
    private void whenSePreguntaSiTieneMeta(TipoObjetivo tipoDado) {
        tieneMeta = tipoDado.tieneMeta();
    }

    private void whenSeDeduceElTipoDe(MiniObjetivoPropuesto... minis) {
        tipo = TipoObjetivo.de(List.of(minis));
    }

    // --- assert ---
    private void thenTieneMeta(boolean esperado) {
        assertThat(tieneMeta).isEqualTo(esperado);
    }

    private void thenElTipoEs(TipoObjetivo esperado) {
        assertThat(tipo).isEqualTo(esperado);
    }
}
