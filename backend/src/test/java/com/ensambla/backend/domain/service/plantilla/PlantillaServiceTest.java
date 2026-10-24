package com.ensambla.backend.domain.service.plantilla;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantillaServiceTest {

    private static final UUID PLANTILLA_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");

    @Mock
    private PlantillaRepository plantillaRepository;

    @InjectMocks
    private PlantillaService plantillaService;

    @Test
    @DisplayName("Devuelve la plantilla activa lista para copiar a un proyecto")
    void obtenerParaProyectoShouldReturnActivePlantilla() {
        PlantillaProyecto plantilla = new PlantillaProyecto(PLANTILLA_ID, "Ventas", "", null);
        when(plantillaRepository.findParaProyectoByIdAndActivaTrue(PLANTILLA_ID)).thenReturn(Optional.of(plantilla));

        PlantillaProyecto result = plantillaService.obtenerParaProyecto(PLANTILLA_ID);

        assertThat(result).isSameAs(plantilla);
    }

    @Test
    @DisplayName("Una plantilla inactiva o inexistente responde como no encontrada")
    void obtenerParaProyectoShouldThrowWhenPlantillaIsMissing() {
        when(plantillaRepository.findParaProyectoByIdAndActivaTrue(PLANTILLA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plantillaService.obtenerParaProyecto(PLANTILLA_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plantilla no encontrada");
    }
}
