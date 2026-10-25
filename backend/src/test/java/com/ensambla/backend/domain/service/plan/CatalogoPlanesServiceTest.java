package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogoPlanesServiceTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final CatalogoPlanesService catalogoPlanesService = new CatalogoPlanesService(planRepository);

    @Test
    @DisplayName("Lista el catálogo en orden gratuito y premium sin consultar negocios")
    void listarShouldReturnOrderedCatalog() {
        givenCatalogoCompleto();

        var result = catalogoPlanesService.listar();

        assertThat(result).hasSize(2);
        verify(planRepository).findByCodigo(CodigoPlan.GRATUITO);
        verify(planRepository).findByCodigo(CodigoPlan.PREMIUM);
    }

    @Test
    @DisplayName("Rechaza un catálogo sin plan gratuito")
    void listarShouldThrowWhenFreePlanIsMissing() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());

        assertThatThrownBy(catalogoPlanesService::listar).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un catálogo incompleto sin inventar el plan premium")
    void listarShouldThrowWhenPremiumIsMissing() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());

        assertThatThrownBy(catalogoPlanesService::listar).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenCatalogoCompleto() {
        var gratuito = plan();
        var premium = plan();
        premium.setCodigo(CodigoPlan.PREMIUM);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
    }
}
