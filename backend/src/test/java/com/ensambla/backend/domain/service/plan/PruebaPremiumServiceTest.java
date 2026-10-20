package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PruebaPremiumServiceTest {

    @Mock
    private PruebaPremiumRepository pruebaPremiumRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @InjectMocks
    private PruebaPremiumService pruebaPremiumService;

    @Test
    @DisplayName("Bloquea el negocio y activa siete días premium sin cambiar el historial de alta")
    void iniciarShouldActivateTrialUnderBusinessLock() {
        Suscripcion suscripcion = givenSuscripcion();
        Plan premium = givenPremium();
        when(pruebaPremiumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PruebaPremium result = pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE);

        assertThat(result).isEqualTo(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE));
        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.PRUEBA);
        assertThat(suscripcion.getPlanId()).isEqualTo(premium.getId());
        assertThat(suscripcion.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(suscripcion.getFinEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(suscripcion.getActualizadoEn()).isEqualTo(INSTANTE);
        var orden = inOrder(usoPlanRepository, suscripcionRepository, pruebaPremiumRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
        orden.verify(pruebaPremiumRepository).findByNegocioId(NEGOCIO_ID);
        orden.verify(pruebaPremiumRepository).save(result);
        orden.verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    @DisplayName("Un negocio no puede reiniciar una prueba usada aunque su suscripción cambie")
    void iniciarShouldRejectUsedTrial() {
        givenSuscripcion();
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID))
                .thenReturn(Optional.of(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE)));

        assertThatThrownBy(() -> pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("PRUEBA_UTILIZADA");

        verifyNoInteractions(planRepository);
        verify(pruebaPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un premium pagado vigente no es reemplazado por una prueba")
    void iniciarShouldRejectActivePremium() {
        Suscripcion suscripcion = givenSuscripcion();
        Plan premium = givenPremium();
        suscripcion.setPlanId(premium.getId());

        assertThatThrownBy(() -> pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("PREMIUM_ACTIVO");
    }

    @Test
    @DisplayName("Un premium vencido que nunca usó prueba puede iniciarla")
    void iniciarShouldAllowExpiredSubscription() {
        Suscripcion suscripcion = givenSuscripcion();
        Plan premium = givenPremium();
        suscripcion.setPlanId(premium.getId());
        suscripcion.setFinEn(INSTANTE);
        when(pruebaPremiumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PruebaPremium result = pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE);

        assertThat(result.paginasIa()).isZero();
    }

    @Test
    @DisplayName("Falla sin suscripción y no consulta el catálogo")
    void iniciarShouldRejectMissingSubscription() {
        assertThatThrownBy(() -> pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");

        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("No inicia la prueba si falta premium en el catálogo")
    void iniciarShouldRejectMissingPremium() {
        givenSuscripcion();

        assertThatThrownBy(() -> pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");

        verify(pruebaPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lista los candidatos vencidos usando el instante recibido")
    void listarVencidasShouldDelegateInstant() {
        when(pruebaPremiumRepository.findNegociosVencidos(INSTANTE)).thenReturn(List.of(NEGOCIO_ID));

        List<UUID> result = pruebaPremiumService.listarVencidas(INSTANTE);

        assertThat(result).containsExactly(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El vencimiento devuelve el gratuito sin borrar el historial de prueba")
    void vencerShouldRestoreFreePlanAtExactEnd() {
        Suscripcion suscripcion = givenSuscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        suscripcion.setFinEn(INSTANTE);
        Plan gratuito = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));

        pruebaPremiumService.vencer(NEGOCIO_ID, INSTANTE);

        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(suscripcion.getPlanId()).isEqualTo(gratuito.getId());
        assertThat(suscripcion.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(suscripcion.getFinEn()).isNull();
        assertThat(suscripcion.getActualizadoEn()).isEqualTo(INSTANTE);
        verify(suscripcionRepository).save(suscripcion);
        verifyNoInteractions(pruebaPremiumRepository);
    }

    @Test
    @DisplayName("El proceso no vence una prueba antes de su fecha de fin")
    void vencerShouldKeepUnexpiredTrial() {
        Suscripcion suscripcion = givenSuscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);

        pruebaPremiumService.vencer(NEGOCIO_ID, INSTANTE);

        verify(suscripcionRepository, never()).save(any());
        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("Un pago activado después de listar candidatos no es degradado por el job")
    void vencerShouldPreserveSubscriptionActivatedAfterSelection() {
        givenSuscripcion();

        pruebaPremiumService.vencer(NEGOCIO_ID, INSTANTE.plusDays(30));

        verify(suscripcionRepository, never()).save(any());
        verifyNoInteractions(planRepository);
    }

    // --- arrange ---
    private Suscripcion givenSuscripcion() {
        Suscripcion suscripcion = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        return suscripcion;
    }

    private Plan givenPremium() {
        Plan premium = plan();
        premium.setId(UUID.randomUUID());
        premium.setCodigo(CodigoPlan.PREMIUM);
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        return premium;
    }
}
