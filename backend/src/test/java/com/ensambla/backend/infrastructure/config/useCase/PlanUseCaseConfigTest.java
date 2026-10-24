package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ConsultaPlanesTestData.premium;
import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.USUARIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanUseCaseConfigTest {

    private final PlanUseCaseConfig planUseCaseConfig = new PlanUseCaseConfig();
    private final Clock clock = Clock.fixed(INSTANTE.toInstant(), ZoneOffset.UTC);
    private final NegocioService negocioService = mock(NegocioService.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);
    private final SuscripcionService suscripcionService =
            planUseCaseConfig.suscripcionService(planRepository, suscripcionRepository);

    @Test
    @DisplayName("El wiring conecta la consulta con usuario, catálogo, uso y reloj")
    void consultarPlanesUseCaseShouldWireDependencies() {
        givenNegocio();
        givenCatalogo();
        Suscripcion suscripcion = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(suscripcion.getPlanId())).thenReturn(Optional.of(plan()));

        var result = planUseCaseConfig.consultarPlanes(negocioService, suscripcionService, planRepository,
                        pruebaPremiumRepository, usoPlanRepository, renovacionPremiumRepository,
                        new TransaccionDirecta(), clock)
                .execute(USUARIO_ID);

        assertThat(result.consultadoEn()).isEqualTo(INSTANTE);
        assertThat(result.planes()).hasSize(2);
    }

    @Test
    @DisplayName("El wiring conecta usuario, catálogo, suscripción y reloj al inicio de prueba")
    void iniciarPruebaUseCaseShouldWireDependencies() {
        givenNegocio();
        givenCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(pruebaPremiumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = planUseCaseConfig.iniciarPrueba(negocioService, suscripcionService, pruebaPremiumRepository,
                        suscripcionRepository, usoPlanRepository, renovacionPremiumRepository,
                        new TransaccionDirecta(), clock)
                .execute(new IniciarPruebaRequest(USUARIO_ID));

        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.limitePaginasIa()).isEqualTo(10);
    }

    @Test
    @DisplayName("El wiring conecta reloj, candidatos y suscripción al vencimiento automático")
    void vencerPruebasUseCaseShouldWireDependencies() {
        when(pruebaPremiumRepository.findNegociosVencidos(INSTANTE)).thenReturn(List.of(NEGOCIO_ID));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));

        planUseCaseConfig.vencerPruebas(suscripcionService, pruebaPremiumRepository, suscripcionRepository,
                usoPlanRepository, new TransaccionDirecta(), clock).execute();

        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void givenCatalogo() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
    }
}
