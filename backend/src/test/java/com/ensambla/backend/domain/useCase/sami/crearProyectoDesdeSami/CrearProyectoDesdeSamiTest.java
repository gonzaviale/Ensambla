package com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProyectoDesdeSamiTest {

    private static final String SIN_NOMBRE = null;
    private static final List<ColumnaSamiItem> SIN_COLUMNAS = List.of();

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final CrearProyectoDesdeSami crearProyecto = new CrearProyectoDesdeSami(negocioRepository,
            plantillaRepository, proyectoRepository, controlPlanService, new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    private ProyectoItem proyecto;
    private Throwable error;

    @Test
    @DisplayName("Guarda en una sola operación la plantilla con las columnas extra y el nombre elegido")
    void guardaLaPlantillaConLasColumnasExtraYElNombre() {
        dadoUnNegocioYLaPlantillaDeInventario();

        cuandoSeCreaElProyecto("  Velas de Ana ", List.of(unaColumna("Saldo pendiente")));

        entoncesSeGuardoConEstructura("Velas de Ana", "Nombre", "Rubro", "Saldo pendiente");
        entoncesElProyectoSeLlama("Velas de Ana");
    }

    @Test
    @DisplayName("Revisa que el plan permita un proyecto más y no usa el guardado sin estructura")
    void revisaElPlanYNoUsaElGuardadoSinEstructura() {
        dadoUnNegocioYLaPlantillaDeInventario();

        cuandoSeCreaElProyecto("Velas de Ana", SIN_COLUMNAS);

        entoncesSeRevisoElPlan();
        entoncesNoSeGuardoSinEstructura();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"   "})
    @DisplayName("Sin nombre o con el nombre en blanco, el proyecto se llama como la plantilla")
    void sinNombreElProyectoSeLlamaComoLaPlantilla(String nombre) {
        dadoUnNegocioYLaPlantillaDeInventario();

        cuandoSeCreaElProyecto(nombre, SIN_COLUMNAS);

        entoncesElProyectoSeLlama("Inventario");
    }

    @Test
    @DisplayName("Si una columna falla no se guarda nada")
    void siUnaColumnaFallaNoSeGuardaNada() {
        dadoUnNegocioYLaPlantillaDeInventario();

        cuandoSeIntentaCrearElProyecto(List.of(unaColumna("Saldo pendiente"), unaColumna(" ")));

        entoncesSeRechazaPor(CampoInvalidoException.class);
        entoncesNoSeGuardoNingunProyecto();
    }

    @Test
    @DisplayName("Si la plantilla no existe no crea nada")
    void siLaPlantillaNoExisteNoCreaNada() {
        dadoUnNegocioSinLaPlantilla();

        cuandoSeIntentaCrearElProyecto(SIN_COLUMNAS);

        entoncesSeRechazaPor(ResourceNotFoundException.class);
        entoncesNoSeTocoNiElPlanNiLosProyectos();
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no consulta plantillas ni crea nada")
    void siElUsuarioNoTieneNegocioNoCreaNada() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaCrearElProyecto(SIN_COLUMNAS);

        entoncesSeRechazaPor(ResourceNotFoundException.class);
        entoncesNoSeConsultaronPlantillas();
        entoncesNoSeTocoNiElPlanNiLosProyectos();
    }

    private static ColumnaSamiItem unaColumna(String nombre) {
        return new ColumnaSamiItem(nombre, "NUMERO", null);
    }

    private void dadoUnNegocioYLaPlantillaDeInventario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaRepository.obtenerParaProyecto(PLANTILLA_ID)).thenReturn(ProyectoTestData.plantilla());
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(guardado -> guardado.getArgument(0));
    }

    private void dadoUnNegocioSinLaPlantilla() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaRepository.obtenerParaProyecto(PLANTILLA_ID)).thenThrow(new ResourceNotFoundException("no"));
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    private void cuandoSeCreaElProyecto(String nombre, List<ColumnaSamiItem> columnas) {
        proyecto = crearProyecto.execute(new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, nombre, columnas));
    }

    private void cuandoSeIntentaCrearElProyecto(List<ColumnaSamiItem> columnas) {
        error = catchThrowable(() -> cuandoSeCreaElProyecto(SIN_NOMBRE, columnas));
    }

    private void entoncesSeGuardoConEstructura(String nombre, String... campos) {
        ArgumentCaptor<Proyecto> guardado = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).saveConEstructura(guardado.capture());
        assertThat(guardado.getValue().getNombre()).isEqualTo(nombre);
        assertThat(guardado.getValue().getBloques().getFirst().getCampos()).extracting(Campo::getNombre)
                .containsExactly(campos);
    }

    private void entoncesElProyectoSeLlama(String nombre) {
        assertThat(proyecto.nombre()).isEqualTo(nombre);
    }

    private void entoncesSeRevisoElPlan() {
        verify(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
    }

    private void entoncesNoSeGuardoSinEstructura() {
        verify(proyectoRepository, never()).save(any());
    }

    private void entoncesSeRechazaPor(Class<? extends Throwable> tipo) {
        assertThat(error).isInstanceOf(tipo);
    }

    private void entoncesNoSeGuardoNingunProyecto() {
        verify(proyectoRepository, never()).saveConEstructura(any());
    }

    private void entoncesNoSeTocoNiElPlanNiLosProyectos() {
        verifyNoInteractions(proyectoRepository, controlPlanService);
    }

    private void entoncesNoSeConsultaronPlantillas() {
        verifyNoInteractions(plantillaRepository);
    }
}