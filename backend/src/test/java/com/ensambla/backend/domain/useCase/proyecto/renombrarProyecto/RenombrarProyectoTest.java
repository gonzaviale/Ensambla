package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RenombrarProyectoTest {

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final RenombrarProyecto renombrarProyecto = new ProyectoUseCaseConfig().renombrarProyecto(
            negocioService,
            new ProyectoService(proyectoRepository, mock(ControlPlanService.class), new TransaccionDirecta()),
            proyectoRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.plusHours(1).toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Renombra el proyecto del negocio de la sesión con la hora del reloj y lo guarda")
    void executeShouldRenameOwnedProyecto() {
        givenNegocio();
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(ProyectoTestData.proyecto()));
        when(proyectoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProyectoItem result = renombrar(" Otro ");

        assertThat(result.id()).isEqualTo(PROYECTO_ID);
        assertThat(result.nombre()).isEqualTo("Otro");
        assertThat(result.actualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    @DisplayName("Renombrar un proyecto ajeno no escribe datos")
    void executeShouldNotWriteForeignProyecto() {
        givenNegocio();
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renombrar("Otro")).isInstanceOf(ResourceNotFoundException.class);
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> renombrar("Nuevo")).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoRepository);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    // --- act ---
    private ProyectoItem renombrar(String nombre) {
        return renombrarProyecto.execute(new RenombrarProyectoRequest(USUARIO_ID, PROYECTO_ID, nombre));
    }
}
