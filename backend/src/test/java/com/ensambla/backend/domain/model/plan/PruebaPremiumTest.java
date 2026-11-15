package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaPremiumTest {

    @Test
    @DisplayName("La prueba dura 168 horas exactas y comienza sin consumo")
    void nuevaShouldCreateSevenDayPeriod() {
        PruebaPremium result = PruebaPremium.nueva(NEGOCIO_ID, INSTANTE);

        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(Duration.between(result.inicioEn(), result.finEn())).isEqualTo(Duration.ofHours(168));
        assertThat(result.paginasIa()).isZero();
    }

    @Test
    @DisplayName("Las páginas admitidas se acumulan hasta diez sin renovar la fecha ni el contador")
    void consumirShouldKeepOriginalPeriodAndAccumulatePages() {
        PruebaPremium prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 8);

        PruebaPremium result = prueba.consumir(2);

        assertThat(result.paginasIa()).isEqualTo(10);
        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(prueba.paginasIa()).isEqualTo(8);
    }

    @Test
    @DisplayName("Se rechaza completo el intento que supera las diez páginas")
    void validarPaginasShouldRejectExceededTrialQuota() {
        PruebaPremium prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 9);

        assertThatThrownBy(() -> prueba.validarPaginas(2)).isInstanceOf(RestriccionPlanException.class)
                .extracting("codigo").isEqualTo("LIMITE_IA_PRUEBA");
    }
}
