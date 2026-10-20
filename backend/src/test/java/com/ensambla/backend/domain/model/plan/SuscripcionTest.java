package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;

class SuscripcionTest {

    @Test
    @DisplayName("El período activo incluye exactamente su inicio")
    void estaVigenteShouldIncludeStart() {
        Suscripcion suscripcion = suscripcion();

        assertThat(suscripcion.estaVigente(INSTANTE)).isTrue();
    }

    @Test
    @DisplayName("Un período futuro no habilita premium antes de su inicio")
    void estaVigenteShouldRejectBeforeStart() {
        Suscripcion suscripcion = suscripcion();

        assertThat(suscripcion.estaVigente(INSTANTE.minusNanos(1))).isFalse();
    }

    @Test
    @DisplayName("Un período deja de estar vigente en el instante exacto de fin")
    void estaVigenteShouldExcludeEnd() {
        Suscripcion suscripcion = suscripcion();

        assertThat(suscripcion.estaVigente(suscripcion.getFinEn())).isFalse();
    }

    @Test
    @DisplayName("El gratuito activo puede tener vigencia sin fecha de fin")
    void estaVigenteShouldAllowNoEnd() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setFinEn(null);

        assertThat(suscripcion.estaVigente(INSTANTE.plusYears(1))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSuscripcion.class, names = {"VENCIDA", "CANCELADA"})
    @DisplayName("Estados sin activación no habilitan premium")
    void estaVigenteShouldRejectInactiveStates(EstadoSuscripcion estado) {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(estado);

        assertThat(suscripcion.estaVigente(INSTANTE)).isFalse();
    }

    @Test
    @DisplayName("La prueba habilita premium desde su inicio y termina exactamente a los siete días")
    void estaVigenteShouldEnableTrialWithinPeriod() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        suscripcion.setFinEn(INSTANTE.plusDays(7));

        assertThat(suscripcion.estaVigente(INSTANTE)).isTrue();
        assertThat(suscripcion.estaVigente(INSTANTE.plusDays(7).minusNanos(1))).isTrue();
        assertThat(suscripcion.estaVigente(INSTANTE.plusDays(7))).isFalse();
        assertThat(suscripcion.estaVigente(INSTANTE.minusNanos(1))).isFalse();
    }
}
