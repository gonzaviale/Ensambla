package com.ensambla.backend.domain.service.proyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProyectoServiceTest {

    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ProyectoService proyectoService = new ProyectoService(proyectoRepository);

    @Test
    @DisplayName("Lista sólo los proyectos del negocio recibido")
    void listarShouldFilterByNegocio() {
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(ProyectoTestData.proyecto()));

        var result = proyectoService.listar(NEGOCIO_ID);

        assertThat(result).hasSize(1);
        verify(proyectoRepository).findAllByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Obtiene un proyecto que pertenece al negocio")
    void obtenerShouldReturnOwnedProyecto() {
        givenProyecto();

        var result = proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID);

        assertThat(result.getId()).isEqualTo(PROYECTO_ID);
    }

    @Test
    @DisplayName("Un proyecto inexistente o ajeno produce el mismo 404")
    void obtenerShouldRejectMissingOrForeignProyecto() {
        givenMissingProyecto();

        assertThatThrownBy(() -> proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Proyecto no encontrado");
    }

    @Test
    @DisplayName("Crear guarda el negocio, nombre y fechas del proyecto en blanco")
    void crearShouldPersistBlankProyecto() {
        when(proyectoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = proyectoService.crear(NEGOCIO_ID, " Nuevo ", "Descripción", AHORA);

        assertThat(result.getNombre()).isEqualTo("Nuevo");
        assertThat(result.getDescripcion()).isEqualTo("Descripción");
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getBloques()).isEmpty();
    }

    @Test
    @DisplayName("Crear desde plantilla guarda toda la estructura en la misma operación")
    void crearDesdePlantillaShouldPersistStructure() {
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = proyectoService.crearDesdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), AHORA);

        assertThat(result.getBloques()).hasSize(2);
        assertThat(result.getPlantillaId()).isEqualTo(PLANTILLA_ID);
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Renombrar valida la pertenencia antes de guardar")
    void renombrarShouldValidateBeforeSaving() {
        givenProyecto();
        when(proyectoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = proyectoService.renombrar(NEGOCIO_ID, PROYECTO_ID, " Otro ", AHORA.plusHours(1));

        assertThat(result.getNombre()).isEqualTo("Otro");
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    @DisplayName("Renombrar un proyecto ajeno no escribe datos")
    void renombrarShouldNotWriteForeignProyecto() {
        givenMissingProyecto();

        assertThatThrownBy(() -> proyectoService.renombrar(NEGOCIO_ID, PROYECTO_ID, "Otro", AHORA))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar valida pertenencia y conserva el filtro de negocio en el borrado")
    void eliminarShouldFilterDeleteByNegocio() {
        givenProyecto();

        proyectoService.eliminar(NEGOCIO_ID, PROYECTO_ID);

        verify(proyectoRepository).deleteByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Eliminar un proyecto ajeno no ejecuta el borrado")
    void eliminarShouldNotDeleteForeignProyecto() {
        givenMissingProyecto();

        assertThatThrownBy(() -> proyectoService.eliminar(NEGOCIO_ID, PROYECTO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(proyectoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    // --- arrange ---
    private void givenProyecto() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(ProyectoTestData.proyecto()));
    }

    private void givenMissingProyecto() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }
}

