package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
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
    @DisplayName("El wiring del listado conecta el servicio")
    void listarPlantillasUseCaseShouldWireService() {
        PlantillaService service = mock(PlantillaService.class);
        when(service.listarActivas()).thenReturn(List.of(Plantilla.builder().nombre("Clientes").build()));
        ListarPlantillasResponse response = config.listarPlantillas(service).execute();

        assertThat(response.content()).extracting("nombre").containsExactly("Clientes");
    }
}
