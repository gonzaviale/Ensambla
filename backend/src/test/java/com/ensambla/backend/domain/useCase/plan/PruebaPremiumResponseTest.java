package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PruebaPremiumResponseTest {

    @Test
    @DisplayName("Mapea fechas, páginas utilizadas y límite total de prueba")
    void deShouldMapTrialDetails() {
        PruebaPremium prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 3);

        PruebaPremiumResponse result = PruebaPremiumResponse.de(prueba);

        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIaUtilizadas()).isEqualTo(3);
        assertThat(result.limitePaginasIa()).isEqualTo(10);
    }
}
