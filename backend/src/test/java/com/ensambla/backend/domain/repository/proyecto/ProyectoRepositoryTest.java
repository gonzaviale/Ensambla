package com.ensambla.backend.domain.repository.proyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProyectoRepositoryTest {

    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene un proyecto que pertenece al negocio")
    void obtenerShouldReturnOwnedProyecto() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(ProyectoTestData.proyecto()));

        var result = proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID);

        assertThat(result.getId()).isEqualTo(PROYECTO_ID);
    }

    @Test
    @DisplayName("Un proyecto inexistente o ajeno produce el mismo 404")
    void obtenerShouldRejectMissingOrForeignProyecto() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Proyecto no encontrado");
    }
}
