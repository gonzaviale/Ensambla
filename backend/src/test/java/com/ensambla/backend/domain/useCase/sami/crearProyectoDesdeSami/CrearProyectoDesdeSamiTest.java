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
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProyectoDesdeSamiTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final CrearProyectoDesdeSami useCase = new CrearProyectoDesdeSami(
            negocioRepository,
            plantillaRepository,
            proyectoRepository,
            controlPlanService,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Guarda en una sola operación la plantilla con las columnas extra y el nombre elegido")
    void executeShouldPersistTemplateWithExtraColumnsAndName() {
        givenNegocioYPlantilla();
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProyectoItem result = useCase.execute(new CrearProyectoDesdeSamiRequest(
                USUARIO_ID, PLANTILLA_ID, "  Velas de Ana ", List.of(columna("Saldo pendiente"))));

        Proyecto guardado = thenProyectoGuardadoConEstructura();
        assertThat(guardado.getNombre()).isEqualTo("Velas de Ana");
        assertThat(guardado.getBloques().getFirst().getCampos()).extracting(Campo::getNombre)
                .containsExactly("Nombre", "Rubro", "Saldo pendiente");
        assertThat(result.nombre()).isEqualTo("Velas de Ana");
        verify(proyectoRepository, never()).save(any());
        verify(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
    }

    @Test
    @DisplayName("Sin nombre o con el nombre en blanco, el proyecto se llama como la plantilla")
    void executeShouldKeepTemplateNameWithoutCustomName() {
        givenNegocioYPlantilla();
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProyectoItem sinNombre = useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, null, List.of()));
        ProyectoItem enBlanco = useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, "   ", List.of()));

        assertThat(sinNombre.nombre()).isEqualTo("Inventario");
        assertThat(enBlanco.nombre()).isEqualTo("Inventario");
    }

    @Test
    @DisplayName("Si una columna falla no se guarda nada")
    void executeShouldNotPersistWhenAColumnFails() {
        givenNegocioYPlantilla();
        List<ColumnaSamiItem> columnas = List.of(columna("Saldo pendiente"), columna(" "));

        assertThatThrownBy(() -> useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, null, columnas)))
                .isInstanceOf(CampoInvalidoException.class);
        verify(proyectoRepository, never()).saveConEstructura(any());
    }

    @Test
    @DisplayName("Si la plantilla no existe no crea nada")
    void executeShouldNotCreateWhenTemplateIsMissing() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaRepository.obtenerParaProyecto(PLANTILLA_ID)).thenThrow(new ResourceNotFoundException("no"));

        assertThatThrownBy(() -> useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, null, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoRepository, controlPlanService);
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no consulta plantillas ni crea nada")
    void executeShouldStopWhenBusinessIsMissing() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, null, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(plantillaRepository, proyectoRepository, controlPlanService);
    }

    // --- arrange ---
    private void givenNegocioYPlantilla() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaRepository.obtenerParaProyecto(PLANTILLA_ID)).thenReturn(ProyectoTestData.plantilla());
    }

    private ColumnaSamiItem columna(String nombre) {
        return new ColumnaSamiItem(nombre, "NUMERO", null);
    }

    // --- assert ---
    private Proyecto thenProyectoGuardadoConEstructura() {
        ArgumentCaptor<Proyecto> guardado = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).saveConEstructura(guardado.capture());
        return guardado.getValue();
    }
}
