package com.ensambla.backend.domain.useCase.proyecto.crearProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProyectoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final CrearProyecto crearProyecto = new ProyectoUseCaseConfig().crearProyecto(
            negocioRepository,
            proyectoRepository,
            controlPlanService,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private ProyectoItem respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Crea el proyecto en blanco del negocio de la sesión con su nombre, descripción y fechas")
    void creaElProyectoEnBlancoDelNegocioDeLaSesionConSuNombreDescripcionYFechas() {
        dadoUnNegocioDelUsuarioQueGuardaLosProyectos();

        cuandoSeCreaElProyecto(" Nuevo ", "Descripción");

        entoncesSeGuardaUnProyectoEnBlancoLlamadoNuevo();
        entoncesSeResponde("Nuevo");
    }

    @Test
    @DisplayName("Valida el cupo de proyectos del plan antes de guardar")
    void validaElCupoDeProyectosDelPlanAntesDeGuardar() {
        dadoUnNegocioDelUsuarioQueGuardaLosProyectos();

        cuandoSeCreaElProyecto("Nuevo", null);

        entoncesSeValidaElCupoAntesDeGuardar();
    }

    @Test
    @DisplayName("Si el plan no deja crear más proyectos no guarda nada")
    void siElPlanNoDejaCrearMasProyectosNoGuardaNada() {
        dadoUnPlanSinCupoDeProyectos();

        cuandoSeIntentaCrearElProyecto("Nuevo", null);

        entoncesSeRechazaPorFaltaDeCupo();
        entoncesNoSeGuardaNingunProyecto();
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void siNoExisteElNegocioNoConsultaNiEscribeProyectos() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaCrearElProyecto("Nuevo", "Descripción");

        entoncesSeRechazaPorqueNoHayNegocio();
        entoncesNoSeTocanLosProyectosNiElPlan();
    }

    // --- dado ---
    private void dadoUnNegocioDelUsuarioQueGuardaLosProyectos() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnPlanSinCupoDeProyectos() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        doThrow(new IllegalStateException("cupo")).when(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    // --- cuando ---
    private void cuandoSeCreaElProyecto(String nombre, String descripcion) {
        respuesta = crearProyecto.execute(new CrearProyectoRequest(USUARIO_ID, nombre, descripcion));
    }

    private void cuandoSeIntentaCrearElProyecto(String nombre, String descripcion) {
        rechazo = catchThrowable(() -> cuandoSeCreaElProyecto(nombre, descripcion));
    }

    // --- entonces ---
    private void entoncesSeGuardaUnProyectoEnBlancoLlamadoNuevo() {
        Proyecto guardado = proyectoGuardado();
        assertThat(guardado.getNombre()).isEqualTo("Nuevo");
        assertThat(guardado.getDescripcion()).isEqualTo("Descripción");
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(guardado.getBloques()).isEmpty();
    }

    private void entoncesSeResponde(String nombre) {
        assertThat(respuesta.nombre()).isEqualTo(nombre);
    }

    private void entoncesSeValidaElCupoAntesDeGuardar() {
        InOrder orden = inOrder(controlPlanService, proyectoRepository);
        orden.verify(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
        orden.verify(proyectoRepository).save(any());
    }

    private void entoncesSeRechazaPorFaltaDeCupo() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesSeRechazaPorqueNoHayNegocio() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNingunProyecto() {
        verifyNoInteractions(proyectoRepository);
    }

    private void entoncesNoSeTocanLosProyectosNiElPlan() {
        verifyNoInteractions(proyectoRepository, controlPlanService);
    }

    private Proyecto proyectoGuardado() {
        ArgumentCaptor<Proyecto> guardado = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).save(guardado.capture());
        return guardado.getValue();
    }
}
