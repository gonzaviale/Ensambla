package com.ensambla.backend.domain.service.plantilla;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
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
    @DisplayName("Devuelve las plantillas activas que entrega el repositorio")
    void listarActivasShouldReturnPlantillasFromRepository() {
        List<Plantilla> plantillas = List.of(new Plantilla());
        when(plantillaRepository.findAllActivas()).thenReturn(plantillas);

        List<Plantilla> result = plantillaService.listarActivas();

        assertThat(result).isSameAs(plantillas);
    }
}
