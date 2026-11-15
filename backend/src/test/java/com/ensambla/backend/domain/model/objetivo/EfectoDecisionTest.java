package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.DecisionTestData.impacto;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class EfectoDecisionTest {

    @Test
    @DisplayName("Un aviso aplicado con el impacto ya medido trae lo que cambió el ingreso semanal")
    void deShouldBeMeasuredWhenTheImpactWasMeasured() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(impacto(), AHORA);

        EfectoDecision result = EfectoDecision.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID), aplicado);

        assertThat(result.estado()).isEqualTo(EstadoEfecto.MEDIDO);
        assertThat(result.deltaIngresoSemanal()).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("Un aviso aplicado de los que se miden, que todavía no terminó la ventana, está midiendo")
    void deShouldBeMeasuringWhenTheWindowHasNotEnded() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);

        EfectoDecision result = EfectoDecision.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID), aplicado);

        assertThat(result.estado()).isEqualTo(EstadoEfecto.MIDIENDO);
        assertThat(result.deltaIngresoSemanal()).isNull();
    }

    @Test
    @DisplayName("Un aviso aplicado de un tipo que no se mide queda sin medición")
    void deShouldHaveNoMeasurementWhenTheAvisoTypeIsNotMeasured() {
        Aviso aplicado = aviso(TipoAviso.COMBO, EstadoAviso.APLICADO);

        EfectoDecision result = EfectoDecision.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID), aplicado);

        assertThat(result).isEqualTo(EfectoDecision.SIN_MEDICION);
    }

    @Test
    @DisplayName("Si el aviso ya no existe no hay con qué medir el efecto")
    void deShouldHaveNoMeasurementWhenTheAvisoWasDeleted() {
        EfectoDecision result = EfectoDecision.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID), null);

        assertThat(result).isEqualTo(EfectoDecision.SIN_MEDICION);
    }

    @Test
    @DisplayName("Un aviso descartado o el paso de un mini-objetivo no se miden")
    void deShouldHaveNoMeasurementForDiscardedAvisosAndSteps() {
        Aviso descartado = aviso(TipoAviso.ESTANCADO, EstadoAviso.DESCARTADO);

        assertThat(EfectoDecision.de(decision(TipoDecision.AVISO_DESCARTADO, AVISO_ID), descartado))
                .isEqualTo(EfectoDecision.SIN_MEDICION);
        assertThat(EfectoDecision.de(decision(TipoDecision.PASO_ACEPTADO, null), null))
                .isEqualTo(EfectoDecision.SIN_MEDICION);
    }
}
