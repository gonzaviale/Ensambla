package com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerProyectoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
    private final ObtenerProyecto obtenerProyecto = new ProyectoUseCaseConfig().obtenerProyecto(
            negocioRepository,
            proyectoRepository);

    private ProyectoItem respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Resuelve el negocio de sesión, delega y mapea la respuesta")
    void resuelveElNegocioDeSesionDelegaYMapeaLaRespuesta() {
        dadoUnNegocioConUnProyecto();

        cuandoSeObtieneElProyecto();

        entoncesSeRespondeConElProyectoDelNegocio();
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void siNoExisteElNegocioNoConsultaNiEscribeProyectos() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaObtenerElProyecto();

        entoncesSeRechazaPorqueNoHayNegocio();
        entoncesNoSeConsultanProyectosNiPlantillas();
    }

    // --- dado ---
    private void dadoUnNegocioConUnProyecto() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto());
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    // --- cuando ---
    private void cuandoSeObtieneElProyecto() {
        respuesta = obtenerProyecto.execute(new ObtenerProyectoRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeIntentaObtenerElProyecto() {
        rechazo = catchThrowable(this::cuandoSeObtieneElProyecto);
    }

    // --- entonces ---
    private void entoncesSeRespondeConElProyectoDelNegocio() {
        assertThat(respuesta.id()).isEqualTo(PROYECTO_ID);
        verify(proyectoRepository).obtener(NEGOCIO_ID, PROYECTO_ID);
    }

    private void entoncesSeRechazaPorqueNoHayNegocio() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeConsultanProyectosNiPlantillas() {
        verifyNoInteractions(proyectoRepository, plantillaRepository);
    }
}
