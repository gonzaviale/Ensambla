package com.ensambla.backend.domain.service.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SistemaServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final UUID PROYECTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final SistemaService sistemaService = new SistemaService(sistemaRepository, proyectoRepository, controlPlanService, usoPlanRepository, new TransaccionDirecta());

    @Test
    @DisplayName("Listar devuelve los sistemas del negocio")
    void listarShouldReturnSistemasOfNegocio() {
        Sistema sistema = givenSistema();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(sistema));

        assertThat(sistemaService.listar(NEGOCIO_ID)).containsExactly(sistema);
    }

    @Test
    @DisplayName("Guardar crea el sistema cuando el proyecto no existe")
    void guardarShouldCreateWhenMissing() {
        Sistema sistema = givenSistema();
        when(sistemaRepository.findNegocioIdByProyectoId(PROYECTO_ID)).thenReturn(Optional.empty());

        assertThat(sistemaService.guardar(NEGOCIO_ID, sistema)).isSameAs(sistema);
        verify(sistemaRepository).guardar(sistema);
    }

    @Test
    @DisplayName("Guardar reemplaza el sistema cuando el proyecto ya es del negocio")
    void guardarShouldReplaceWhenOwned() {
        Sistema sistema = givenSistema();
        when(sistemaRepository.findNegocioIdByProyectoId(PROYECTO_ID)).thenReturn(Optional.of(NEGOCIO_ID));

        sistemaService.guardar(NEGOCIO_ID, sistema);

        verify(sistemaRepository).guardar(sistema);
    }

    @Test
    @DisplayName("Guardar responde como si no existiera cuando el proyecto es de otro negocio")
    void guardarShouldHideForeignProyecto() {
        Sistema sistema = givenSistema();
        when(sistemaRepository.findNegocioIdByProyectoId(PROYECTO_ID)).thenReturn(Optional.of(OTRO_NEGOCIO_ID));

        assertThatThrownBy(() -> sistemaService.guardar(NEGOCIO_ID, sistema))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sistema no encontrado");
        verify(sistemaRepository, never()).guardar(sistema);
    }

    @Test
    @DisplayName("Eliminar borra el proyecto del negocio sin comprobar antes que exista")
    void eliminarShouldDeleteWithoutCheckingExistence() {
        sistemaService.eliminar(NEGOCIO_ID, PROYECTO_ID);

        verify(proyectoRepository).deleteByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Guardar valida sólo el incremento de registros del sistema completo")
    void guardarShouldValidateOnlyAdditionalRows() {
        Sistema vacio = givenSistema();
        Sistema conFilas = new Sistema(vacio.proyecto(), List.of(), Map.of(UUID.randomUUID(),
                List.of(Registro.builder().build(), Registro.builder().build())));
        when(sistemaRepository.findNegocioIdByProyectoId(PROYECTO_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(usoPlanRepository.contarRegistrosProyecto(NEGOCIO_ID, PROYECTO_ID)).thenReturn(1L);

        sistemaService.guardar(NEGOCIO_ID, conFilas);

        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, null);
        verify(sistemaRepository).guardar(conFilas);
    }

    // --- arrange ---
    private Sistema givenSistema() {
        return new Sistema(Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build(), List.of(), Map.of());
    }
}
