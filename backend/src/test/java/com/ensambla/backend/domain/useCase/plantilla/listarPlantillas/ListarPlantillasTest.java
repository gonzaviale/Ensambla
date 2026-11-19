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
    private ListarPlantillasResponse respuesta;

    @BeforeEach
    void setUp() {
        listarPlantillas = new ListarPlantillas(plantillaRepository);
    }

    @Test
    @DisplayName("Lista las plantillas activas y las devuelve mapeadas")
    void listaLasPlantillasActivasYLasDevuelveMapeadas() {
        dadoUnaPlantillaActivaDePedidos();

        cuandoSeListanLasPlantillas();

        entoncesSeDevuelveLaPlantillaDePedidos();
    }

    // --- dado ---

    private void dadoUnaPlantillaActivaDePedidos() {
        List<Plantilla> activas = List.of(Plantilla.builder().id(PLANTILLA_ID).nombre(NOMBRE).build());
        when(plantillaRepository.findAllActivas()).thenReturn(activas);
    }

    // --- cuando ---

    private void cuandoSeListanLasPlantillas() {
        respuesta = listarPlantillas.execute();
    }

    // --- entonces ---

    private void entoncesSeDevuelveLaPlantillaDePedidos() {
        assertThat(respuesta.content()).hasSize(1);
        assertThat(respuesta.content().getFirst().id()).isEqualTo(PLANTILLA_ID);
        assertThat(respuesta.content().getFirst().nombre()).isEqualTo(NOMBRE);
    }
}
