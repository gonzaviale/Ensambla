package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RenombrarProyectoTest {

    private static final OffsetDateTime UNA_HORA_DESPUES = AHORA.plusHours(1);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class, CALLS_REAL_METHODS);
    private final RenombrarProyecto renombrarProyecto = new ProyectoUseCaseConfig().renombrarProyecto(
            negocioRepository,
            proyectoRepository,
            new TransaccionDirecta(),
            Clock.fixed(UNA_HORA_DESPUES.toInstant(), ZoneOffset.UTC));

    private ProyectoItem respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Renombra el proyecto del negocio de la sesión con la hora del reloj y lo guarda")
    void renombraElProyectoDelNegocioDeLaSesionConLaHoraDelRelojYLoGuarda() {
        dadoUnNegocioConUnProyectoQueSeGuarda();

        cuandoSeRenombraElProyecto(" Otro ");

        entoncesElProyectoSeLlamaOtroYSeActualizoConLaHoraDelReloj();
    }

    @Test
    @DisplayName("Renombrar un proyecto ajeno no escribe datos")
    void renombrarUnProyectoAjenoNoEscribeDatos() {
        dadoUnNegocioConUnProyectoAjeno();

        cuandoSeIntentaRenombrarElProyecto("Otro");

        entoncesSeRechazaPorqueElProyectoNoExiste();
        entoncesNoSeGuardaNingunProyecto();
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void siNoExisteElNegocioNoConsultaNiEscribeProyectos() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaRenombrarElProyecto("Nuevo");

        entoncesSeRechazaPorqueNoHayNegocio();
        entoncesNoSeTocanLosProyectos();
    }

    // --- dado ---
    private void dadoUnNegocioConUnProyectoQueSeGuarda() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(proyecto()));
        when(proyectoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnNegocioConUnProyectoAjeno() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    // --- cuando ---
    private void cuandoSeRenombraElProyecto(String nombre) {
        respuesta = renombrarProyecto.execute(new RenombrarProyectoRequest(USUARIO_ID, PROYECTO_ID, nombre));
    }

    private void cuandoSeIntentaRenombrarElProyecto(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeRenombraElProyecto(nombre));
    }

    // --- entonces ---
    private void entoncesElProyectoSeLlamaOtroYSeActualizoConLaHoraDelReloj() {
        assertThat(respuesta.id()).isEqualTo(PROYECTO_ID);
        assertThat(respuesta.nombre()).isEqualTo("Otro");
        assertThat(respuesta.actualizadoEn()).isEqualTo(UNA_HORA_DESPUES);
    }

    private void entoncesSeRechazaPorqueElProyectoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeRechazaPorqueNoHayNegocio() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNingunProyecto() {
        verify(proyectoRepository, never()).save(any());
    }

    private void entoncesNoSeTocanLosProyectos() {
        verifyNoInteractions(proyectoRepository);
    }
}
