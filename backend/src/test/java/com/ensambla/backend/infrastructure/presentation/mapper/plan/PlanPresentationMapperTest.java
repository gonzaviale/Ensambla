package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.dto.plan.IniciarPruebaRequest;
import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PlanPresentationMapperTest {

    private final PlanPresentationMapper planPresentationMapper = new PlanPresentationMapper();

    @Test
    @DisplayName("El pedido recibe exclusivamente el identificador del usuario autenticado")
    void toRequestShouldMapAuthenticatedUser() {
        IniciarPruebaRequest result = planPresentationMapper.toRequest(NEGOCIO_ID);

        assertThat(result.usuarioId()).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("La respuesta HTTP contiene fechas y cuotas de la prueba")
    void toResponseShouldMapEveryField() {
        PruebaPremiumResponse response = new PruebaPremiumResponse(INSTANTE, INSTANTE.plusDays(7), 2, 10);

        PruebaResponse result = planPresentationMapper.toResponse(response);

        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIaUtilizadas()).isEqualTo(2);
        assertThat(result.limitePaginasIa()).isEqualTo(10);
    }
}
