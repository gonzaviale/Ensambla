package com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProyectoDesdeSamiTest {

    private final NegocioService negocioService = mock(NegocioService.class);
    private final PlantillaService plantillaService = mock(PlantillaService.class);
    private final ProyectoService proyectoService = mock(ProyectoService.class);
    private final CrearProyectoDesdeSami useCase = new CrearProyectoDesdeSami(
            negocioService,
            plantillaService,
            proyectoService,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Resuelve el negocio de la sesión, crea el proyecto con las columnas y devuelve su estructura")
    void executeShouldCreateProjectWithExtraColumns() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaService.obtenerParaProyecto(PLANTILLA_ID)).thenReturn(ProyectoTestData.plantilla());
        List<ConfiguracionCampo> esperadas = List.of(
                new ConfiguracionCampo("Saldo pendiente", "NUMERO", false, null, null, null, null));
        when(proyectoService.crearDesdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), "Velas", esperadas, AHORA))
                .thenReturn(ProyectoTestData.proyecto());

        ProyectoItem result = useCase.execute(new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, "Velas",
                List.of(new ColumnaSamiItem("Saldo pendiente", "NUMERO", null))));

        assertThat(result.id()).isEqualTo(PROYECTO_ID);
        verify(proyectoService).crearDesdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), "Velas", esperadas, AHORA);
    }

    @Test
    @DisplayName("Si la plantilla no existe no crea nada")
    void executeShouldNotCreateWhenTemplateIsMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaService.obtenerParaProyecto(PLANTILLA_ID)).thenThrow(new ResourceNotFoundException("no"));

        assertThatThrownBy(() -> useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, null, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoService);
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no consulta plantillas ni crea nada")
    void executeShouldStopWhenBusinessIsMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> useCase.execute(
                new CrearProyectoDesdeSamiRequest(USUARIO_ID, PLANTILLA_ID, null, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(plantillaService, proyectoService);
        verify(proyectoService, org.mockito.Mockito.never())
                .crearDesdePlantilla(any(), any(), any(), any(), any());
    }
}
