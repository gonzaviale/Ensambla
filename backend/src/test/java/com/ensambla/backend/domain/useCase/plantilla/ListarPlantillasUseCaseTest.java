package com.ensambla.backend.domain.useCase.plantilla;

import com.ensambla.backend.domain.dto.plantilla.ListarPlantillasResponse;
import com.ensambla.backend.domain.mapper.plantilla.ListarPlantillasMapper;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarPlantillasUseCaseTest {

    private static final UUID PLANTILLA_ID = UUID.randomUUID();
    private static final String NOMBRE = "Pedidos";

    @Mock
    private PlantillaService plantillaService;

    private ListarPlantillasUseCase listarPlantillasUseCase;

    @BeforeEach
    void setUp() {
        listarPlantillasUseCase = new ListarPlantillasUseCase(plantillaService, new ListarPlantillasMapper());
    }

    @Test
    @DisplayName("Lista las plantillas activas y las devuelve mapeadas")
    void executeShouldReturnMappedPlantillas() {
        givenServiceListsPlantillas(List.of(Plantilla.builder().id(PLANTILLA_ID).nombre(NOMBRE).build()));

        ListarPlantillasResponse response = listarPlantillasUseCase.execute();

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(PLANTILLA_ID);
        assertThat(response.content().getFirst().nombre()).isEqualTo(NOMBRE);
    }

    // --- arrange ---
    private void givenServiceListsPlantillas(List<Plantilla> plantillas) {
        when(plantillaService.listarActivas()).thenReturn(plantillas);
    }
}
