package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillasResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlantillaUseCaseConfigTest {

    private final PlantillaUseCaseConfig config = new PlantillaUseCaseConfig();

    @Test
    @DisplayName("El wiring del listado conecta el repositorio")
    void listarPlantillasUseCaseShouldWireRepository() {
        PlantillaRepository repository = mock(PlantillaRepository.class);
        when(repository.findAllActivas()).thenReturn(List.of(Plantilla.builder().nombre("Clientes").build()));

        ListarPlantillasResponse response = config.listarPlantillas(repository).execute();

        assertThat(response.content()).extracting("nombre").containsExactly("Clientes");
    }
}
