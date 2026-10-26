package com.ensambla.backend.domain.repository.plantilla;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantillaRepositoryTest {

    private static final PlantillaId PLANTILLA_ID = PlantillaId.de("e0000000-0000-4000-8000-000000000001");

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private PlantillaRepository plantillaRepository;

    @Test
    @DisplayName("Devuelve la plantilla activa lista para copiar a un proyecto")
    void obtenerParaProyectoShouldReturnActivePlantilla() {
        PlantillaProyecto plantilla = new PlantillaProyecto(PLANTILLA_ID, "Ventas", "", null);
        when(plantillaRepository.findParaProyectoByIdAndActivaTrue(PLANTILLA_ID)).thenReturn(Optional.of(plantilla));

        PlantillaProyecto result = plantillaRepository.obtenerParaProyecto(PLANTILLA_ID);

        assertThat(result).isSameAs(plantilla);
    }

    @Test
    @DisplayName("Una plantilla inactiva o inexistente responde como no encontrada")
    void obtenerParaProyectoShouldThrowWhenPlantillaIsMissing() {
        when(plantillaRepository.findParaProyectoByIdAndActivaTrue(PLANTILLA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plantillaRepository.obtenerParaProyecto(PLANTILLA_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plantilla no encontrada");
    }
}
