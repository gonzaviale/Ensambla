package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarProyectoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class, CALLS_REAL_METHODS);
    private final EliminarProyecto eliminarProyecto = new ProyectoUseCaseConfig().eliminarProyecto(
            negocioRepository,
            proyectoRepository,
            new TransaccionDirecta());

    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Eliminar valida pertenencia y borra filtrando por el negocio de la sesión")
    void eliminarValidaPertenenciaYBorraFiltrandoPorElNegocioDeLaSesion() {
        dadoUnProyectoDelNegocio();

        cuandoSeEliminaElProyecto();

        entoncesSeBorraElProyectoDelNegocio();
    }

    @Test
    @DisplayName("Eliminar un proyecto ajeno no ejecuta el borrado")
    void eliminarUnProyectoAjenoNoEjecutaElBorrado() {
        dadoUnProyectoAjeno();

        cuandoSeIntentaEliminarElProyecto();

        entoncesSeRechazaPorqueElProyectoNoExiste();
        entoncesNoSeBorraNingunProyecto();
    }

    // --- dado ---
    private void dadoUnProyectoDelNegocio() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(proyecto()));
    }

    private void dadoUnProyectoAjeno() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeEliminaElProyecto() {
        eliminarProyecto.execute(new EliminarProyectoRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeIntentaEliminarElProyecto() {
        rechazo = catchThrowable(this::cuandoSeEliminaElProyecto);
    }

    // --- entonces ---
    private void entoncesSeBorraElProyectoDelNegocio() {
        verify(proyectoRepository).deleteByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    private void entoncesSeRechazaPorqueElProyectoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeBorraNingunProyecto() {
        verify(proyectoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }
}
