package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.plan.CatalogoPlanesMapper;
import com.ensambla.backend.domain.service.plan.CatalogoPlanesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarCatalogoPlanesUseCaseTest {

    private final CatalogoPlanesService catalogoPlanesService = mock(CatalogoPlanesService.class);
    private final ListarCatalogoPlanesUseCase listarCatalogoPlanesUseCase =
            new ListarCatalogoPlanesUseCase(catalogoPlanesService, new CatalogoPlanesMapper());

    @Test
    @DisplayName("Delega la consulta global y devuelve DTOs del catálogo")
    void executeShouldReturnCatalog() {
        when(catalogoPlanesService.listar()).thenReturn(List.of(plan()));

        var result = listarCatalogoPlanesUseCase.execute();

        assertThat(result.getFirst().codigo()).isEqualTo("GRATUITO");
        verify(catalogoPlanesService).listar();
    }

    @Test
    @DisplayName("Propaga el catálogo incompleto sin sustituir condiciones comerciales")
    void executeShouldPropagateMissingCatalog() {
        var error = new ResourceNotFoundException("Plan no encontrado");
        when(catalogoPlanesService.listar()).thenThrow(error);

        assertThatThrownBy(listarCatalogoPlanesUseCase::execute).isSameAs(error);
    }
}
