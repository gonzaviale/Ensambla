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
    void soloElMonetarioTieneMetaEnPesos(TipoObjetivo tipoDado, boolean esperado) {
        cuandoSePreguntaSiTieneMeta(tipoDado);

        entoncesTieneMeta(esperado);
    }

    @Test
    @DisplayName("Con sólo mini-objetivos medidos el objetivo es con datos")
    void conSoloMiniObjetivosMedidosElObjetivoEsConDatos() {
        cuandoSeDeduceElTipoDe(MEDIDO, MEDIDO);

        entoncesElTipoEs(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo mini-objetivos manuales el objetivo es sin datos")
    void conSoloMiniObjetivosManualesElObjetivoEsSinDatos() {
        cuandoSeDeduceElTipoDe(MANUAL);

        entoncesElTipoEs(TipoObjetivo.SIN_DATOS);
    }

    @Test
    @DisplayName("Con mini-objetivos medidos y manuales el objetivo es híbrido")
    void conMiniObjetivosMedidosYManualesElObjetivoEsHibrido() {
        cuandoSeDeduceElTipoDe(MEDIDO, MANUAL);

        entoncesElTipoEs(TipoObjetivo.HIBRIDO);
    }

    // --- act ---
    private void cuandoSePreguntaSiTieneMeta(TipoObjetivo tipoDado) {
        tieneMeta = tipoDado.tieneMeta();
    }

    private void cuandoSeDeduceElTipoDe(MiniObjetivoPropuesto... minis) {
        tipo = TipoObjetivo.de(List.of(minis));
    }

    // --- assert ---
    private void entoncesTieneMeta(boolean esperado) {
        assertThat(tieneMeta).isEqualTo(esperado);
    }

    private void entoncesElTipoEs(TipoObjetivo esperado) {
        assertThat(tipo).isEqualTo(esperado);
    }
}
