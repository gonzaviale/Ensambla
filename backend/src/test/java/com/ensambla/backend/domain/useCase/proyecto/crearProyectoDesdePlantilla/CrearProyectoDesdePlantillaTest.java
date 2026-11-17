package com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.plantilla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProyectoDesdePlantillaTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final CrearProyectoDesdePlantilla crearProyectoDesdePlantilla =
            new ProyectoUseCaseConfig().crearProyectoDesdePlantilla(
                    negocioRepository,
                    plantillaRepository,
                    proyectoRepository,
                    controlPlanService,
                    new TransaccionDirecta(),
                    Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private ProyectoItem respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda toda la estructura de la plantilla en la misma operación, sin guardar el proyecto aparte")
    void guardaTodaLaEstructuraDeLaPlantillaEnLaMismaOperacionSinGuardarElProyectoAparte() {
        dadoUnNegocioYUnaPlantillaQueSeGuardaConSuEstructura();

        cuandoSeCreaElProyectoDesdeLaPlantilla();

        entoncesSeRespondeConLosDosBloquesDeLaPlantilla();
        entoncesSeGuardaLaEstructuraDelNegocio();
        entoncesNoSeGuardaElProyectoAparte();
    }

    @Test
    @DisplayName("Valida el cupo de proyectos del plan antes de guardar")
    void validaElCupoDeProyectosDelPlanAntesDeGuardar() {
        dadoUnNegocioYUnaPlantillaQueSeGuardaConSuEstructura();

        cuandoSeCreaElProyectoDesdeLaPlantilla();

        entoncesSeValidaElCupoAntesDeGuardar();
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void siNoExisteElNegocioNoConsultaNiEscribeProyectos() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaCrearElProyectoDesdeLaPlantilla();

        entoncesSeRechazaPorqueNoHayNegocio();
        entoncesNoSeTocaNingunaOtraDependencia();
    }

    // --- dado ---
    private void dadoUnNegocioYUnaPlantillaQueSeGuardaConSuEstructura() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaRepository.obtenerParaProyecto(PLANTILLA_ID)).thenReturn(plantilla());
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    // --- cuando ---
    private void cuandoSeCreaElProyectoDesdeLaPlantilla() {
        respuesta = crearProyectoDesdePlantilla.execute(
                new CrearProyectoDesdePlantillaRequest(USUARIO_ID, PLANTILLA_ID));
    }

    private void cuandoSeIntentaCrearElProyectoDesdeLaPlantilla() {
        rechazo = catchThrowable(this::cuandoSeCreaElProyectoDesdeLaPlantilla);
    }

    // --- entonces ---
    private void entoncesSeRespondeConLosDosBloquesDeLaPlantilla() {
        assertThat(respuesta.bloques()).hasSize(2);
        assertThat(respuesta.plantillaId()).isEqualTo(PLANTILLA_ID);
    }

    private void entoncesSeGuardaLaEstructuraDelNegocio() {
        ArgumentCaptor<Proyecto> guardado = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).saveConEstructura(guardado.capture());
        assertThat(guardado.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesNoSeGuardaElProyectoAparte() {
        verify(proyectoRepository, never()).save(any());
    }

    private void entoncesSeValidaElCupoAntesDeGuardar() {
        InOrder orden = inOrder(controlPlanService, proyectoRepository);
        orden.verify(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
        orden.verify(proyectoRepository).saveConEstructura(any());
    }

    private void entoncesSeRechazaPorqueNoHayNegocio() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeTocaNingunaOtraDependencia() {
        verifyNoInteractions(proyectoRepository, plantillaRepository, controlPlanService);
    }
}
