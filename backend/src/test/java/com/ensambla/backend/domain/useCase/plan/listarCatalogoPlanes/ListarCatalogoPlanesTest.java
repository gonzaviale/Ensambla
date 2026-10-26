package com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarCatalogoPlanesTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private PlanRepository planRepository;

    private ListarCatalogoPlanes listarCatalogoPlanes;

    @BeforeEach
    void setUp() {
        listarCatalogoPlanes = new ListarCatalogoPlanes(planRepository, new TransaccionDirecta());
    }

    @Test
    @DisplayName("Consulta los planes globales en orden sin pedir datos de un negocio")
    void executeShouldReturnOrderedGlobalCatalog() {
        givenCatalogoCompleto();

        ListarCatalogoPlanesResponse result = listarCatalogoPlanes.execute();

        thenCatalogoOrdenado(result);
    }

    @Test
    @DisplayName("Propaga el plan gratuito faltante sin inventar un catálogo")
    void executeShouldThrowWhenFreePlanIsMissing() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());

        assertThatThrownBy(listarCatalogoPlanes::execute).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Propaga el premium faltante sin devolver un catálogo incompleto")
    void executeShouldThrowWhenPremiumIsMissing() {
        givenCatalogoSinPremium();

        assertThatThrownBy(listarCatalogoPlanes::execute).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenCatalogoCompleto() {
        Plan gratuito = plan();
        Plan premium = plan();
        premium.setCodigo(CodigoPlan.PREMIUM);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
    }

    private void givenCatalogoSinPremium() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());
    }

    // --- assert ---
    private void thenCatalogoOrdenado(ListarCatalogoPlanesResponse result) {
        assertThat(result.planes()).extracting(PlanCatalogoItem::codigo).containsExactly("GRATUITO", "PREMIUM");
        verify(planRepository).findByCodigo(CodigoPlan.GRATUITO);
        verify(planRepository).findByCodigo(CodigoPlan.PREMIUM);
    }
}
