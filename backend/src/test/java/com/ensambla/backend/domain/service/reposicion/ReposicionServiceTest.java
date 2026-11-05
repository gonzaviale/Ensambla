package com.ensambla.backend.domain.service.reposicion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ReposicionDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.datosCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReposicionServiceTest {

    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);

    private ReposicionService reposicionService;

    @BeforeEach
    void setUp() {
        reposicionService = new ReposicionService(proyectoRepository, datosDeProyectoService);
    }

    @Test
    @DisplayName("Devuelve los productos que hay que reponer del sistema del negocio")
    void faltantesDeShouldReturnTheMissingProductsOfTheProject() {
        givenUnSistemaConUnProductoAgotado();

        Faltantes result = reposicionService.faltantesDe(NEGOCIO_ID, PROYECTO_ID);

        assertThat(result.items()).singleElement()
                .satisfies(item -> assertThat(item.productoId()).isEqualTo(YERBA_ID));
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente y no carga datos")
    void faltantesDeShouldNotFindAForeignProject() {
        givenElSistemaEsDeOtroNegocio();

        assertThatThrownBy(() -> reposicionService.faltantesDe(NEGOCIO_ID, PROYECTO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(datosDeProyectoService);
    }

    // --- arrange ---
    private void givenUnSistemaConUnProductoAgotado() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(
                datosCon(inventarioCon(producto(YERBA_ID, "Yerba", 0, 10, null))));
    }

    private void givenElSistemaEsDeOtroNegocio() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }
}
