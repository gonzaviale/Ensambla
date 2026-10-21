package com.ensambla.backend.domain.mapper.plan;

import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class IniciarPruebaMapperTest {

    private final IniciarPruebaMapper iniciarPruebaMapper = new IniciarPruebaMapper();

    @Test
    @DisplayName("Mapea fechas, páginas utilizadas y límite total de prueba")
    void toResponseShouldMapTrialDetails() {
        PruebaPremium prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 3);

        PruebaPremiumResponse result = iniciarPruebaMapper.toResponse(prueba);

        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIaUtilizadas()).isEqualTo(3);
        assertThat(result.limitePaginasIa()).isEqualTo(10);
    }
}
