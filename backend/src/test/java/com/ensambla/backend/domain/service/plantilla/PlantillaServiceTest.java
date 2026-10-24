package com.ensambla.backend.domain.service.plantilla;

import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantillaServiceTest {

    @Mock
    private PlantillaRepository plantillaRepository;

    @InjectMocks
    private PlantillaService plantillaService;

    @Test
    @DisplayName("Devuelve las plantillas activas listas para copiar a un proyecto")
    void listarParaProyectoShouldReturnPlantillasFromRepository() {
        List<PlantillaProyecto> plantillas = List.of(new PlantillaProyecto(null, "Ventas", "", null));
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(plantillas);

        List<PlantillaProyecto> result = plantillaService.listarParaProyecto();

        assertThat(result).isSameAs(plantillas);
    }
}
