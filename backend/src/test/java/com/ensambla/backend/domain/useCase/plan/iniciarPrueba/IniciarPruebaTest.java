package com.ensambla.backend.domain.useCase.plan.iniciarPrueba;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IniciarPruebaTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private final NegocioService negocioService = mock(NegocioService.class);
    private final PruebaPremiumService pruebaPremiumService = mock(PruebaPremiumService.class);
    private final IniciarPrueba iniciarPrueba = new IniciarPrueba(
            negocioService, pruebaPremiumService,
            Clock.fixed(INSTANTE.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Inicia para el negocio del usuario con el reloj del servidor")
    void executeShouldResolveBusinessAndServerInstant() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE))
                .thenReturn(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE));

        PruebaPremiumResponse result = iniciarPrueba.execute(new IniciarPruebaRequest(USUARIO_ID));

        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIaUtilizadas()).isZero();
        assertThat(result.limitePaginasIa()).isEqualTo(10);
        verify(pruebaPremiumService).iniciar(NEGOCIO_ID, INSTANTE);
    }

    @Test
    @DisplayName("Propaga el rechazo de una prueba ya utilizada")
    void executeShouldPropagateTrialRestriction() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        doThrow(new RestriccionPlanException("PRUEBA_UTILIZADA", "Ya utilizada"))
                .when(pruebaPremiumService).iniciar(NEGOCIO_ID, INSTANTE);

        assertThatThrownBy(() -> iniciarPrueba.execute(new IniciarPruebaRequest(USUARIO_ID)))
                .isInstanceOf(RestriccionPlanException.class).hasMessage("Ya utilizada");
    }
}
