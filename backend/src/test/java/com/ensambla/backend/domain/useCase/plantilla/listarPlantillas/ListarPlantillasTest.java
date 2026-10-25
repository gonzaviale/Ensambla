package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarPlantillasTest {

    private static final PlantillaId PLANTILLA_ID = PlantillaId.nuevo();
    private static final String NOMBRE = "Pedidos";

    @Mock
    private PlantillaRepository plantillaRepository;

    private ListarPlantillas listarPlantillas;

    @BeforeEach
    void setUp() {
        listarPlantillas = new ListarPlantillas(plantillaRepository);
    }

    @Test
    @DisplayName("Lista las plantillas activas y las devuelve mapeadas")
    void executeShouldReturnMappedPlantillas() {
        givenRepositoryListsPlantillas(List.of(Plantilla.builder().id(PLANTILLA_ID).nombre(NOMBRE).build()));

        ListarPlantillasResponse response = listarPlantillas.execute();

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(PLANTILLA_ID);
        assertThat(response.content().getFirst().nombre()).isEqualTo(NOMBRE);
    }

    // --- arrange ---
    private void givenRepositoryListsPlantillas(List<Plantilla> plantillas) {
        when(plantillaRepository.findAllActivas()).thenReturn(plantillas);
    }
}
