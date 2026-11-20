package com.ensambla.backend.domain.repository.proyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProyectoRepositoryTest {

    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class, CALLS_REAL_METHODS);

    private Proyecto obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Obtiene un proyecto que pertenece al negocio")
    void obtieneUnProyectoQuePerteneceAlNegocio() {
        dadoUnProyectoDelNegocio();

        cuandoSeObtieneElProyecto();

        entoncesSeDevuelveElProyecto();
    }

    @Test
    @DisplayName("Un proyecto inexistente o ajeno produce el mismo 404")
    void unProyectoInexistenteOAjenoProduceElMismo404() {
        dadoQueElProyectoNoExisteOEsAjeno();

        cuandoSeIntentaObtenerElProyecto();

        entoncesSeRespondeQueElProyectoNoSeEncontro();
    }

    // --- dado ---

    private void dadoUnProyectoDelNegocio() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(ProyectoTestData.proyecto()));
    }

    private void dadoQueElProyectoNoExisteOEsAjeno() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtieneElProyecto() {
        obtenido = proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID);
    }

    private void cuandoSeIntentaObtenerElProyecto() {
        rechazo = catchThrowable(this::cuandoSeObtieneElProyecto);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElProyecto() {
        assertThat(obtenido.getId()).isEqualTo(PROYECTO_ID);
    }

    private void entoncesSeRespondeQueElProyectoNoSeEncontro() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Proyecto no encontrado");
    }
}
