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
    private final PlantillaRepository repository = mock(PlantillaRepository.class);
    private ListarPlantillasResponse plantillas;

    @Test
    @DisplayName("El wiring del listado conecta el repositorio")
    void elWiringDelListadoConectaElRepositorio() {
        dadaLaPlantillaActiva("Clientes");

        cuandoSeListanLasPlantillas();

        entoncesSeListan("Clientes");
    }

    private void dadaLaPlantillaActiva(String nombre) {
        when(repository.findAllActivas()).thenReturn(List.of(Plantilla.builder().nombre(nombre).build()));
    }

    private void cuandoSeListanLasPlantillas() {
        plantillas = config.listarPlantillas(repository).execute();
    }

    private void entoncesSeListan(String... nombres) {
        assertThat(plantillas.content()).extracting("nombre").containsExactly((Object[]) nombres);
    }
}