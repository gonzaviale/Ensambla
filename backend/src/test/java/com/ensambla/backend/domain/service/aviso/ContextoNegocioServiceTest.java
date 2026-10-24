package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextoNegocioServiceTest {

    private static final UUID NEGOCIO_ID = AvisoTestData.NEGOCIO_ID;
    private static final UUID PROYECTO_ID = AvisoTestData.PROYECTO_ID;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @InjectMocks
    private ContextoNegocioService contextoNegocioService;

    @Test
    @DisplayName("Arma las tablas de inventario con sus campos por clave y sus registros")
    void armarShouldBuildInventariosWithCamposAndRegistros() {
        Registro registro = AvisoTestData.registro(UUID.randomUUID(), "Yerba", 2, 5);
        givenProyecto(AvisoTestData.bloqueInventario());
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(registro));

        ContextoNegocio contexto = contextoNegocioService.armar(NEGOCIO_ID);

        assertThat(contexto.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(contexto.inventarios()).singleElement().satisfies(inventario -> {
            assertThat(inventario.bloque().getId()).isEqualTo(AvisoTestData.INVENTARIO_ID);
            assertThat(inventario.camposPorClave()).containsOnlyKeys("name", "quantity", "minimum");
            assertThat(inventario.registros()).containsExactly(registro);
            assertThat(inventario.entradasId()).isNull();
        });
    }

    @Test
    @DisplayName("Ubica la tabla de entradas del proyecto y deja afuera las tablas que no son de inventario")
    void armarShouldFindEntradasAndIgnoreOtherBlocks() {
        Bloque entradas = Bloque.builder().id(AvisoTestData.ENTRADAS_ID).negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID).tipo(TipoBloque.ENTRADAS).campos(List.of()).build();
        givenProyecto(AvisoTestData.bloqueInventario(), entradas);
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of());

        ContextoNegocio contexto = contextoNegocioService.armar(NEGOCIO_ID);

        assertThat(contexto.inventarios()).extracting(BloqueInventario::entradasId)
                .containsExactly(AvisoTestData.ENTRADAS_ID);
    }

    @Test
    @DisplayName("Ignora los campos sin clave semántica")
    void armarShouldIgnoreCamposWithoutClave() {
        Bloque bloque = AvisoTestData.bloqueInventario();
        bloque.setCampos(List.of(AvisoTestData.campo(AvisoTestData.CAMPO_NOMBRE_ID, "name"),
                Campo.builder().id(UUID.randomUUID()).clave(null).build()));
        givenProyecto(bloque);
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of());

        ContextoNegocio contexto = contextoNegocioService.armar(NEGOCIO_ID);

        assertThat(contexto.inventarios().get(0).camposPorClave()).containsOnlyKeys("name");
    }

    @Test
    @DisplayName("Un negocio sin proyectos queda sin tablas de inventario")
    void armarShouldReturnNoInventariosWhenNegocioHasNoProyectos() {
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());

        assertThat(contextoNegocioService.armar(NEGOCIO_ID).inventarios()).isEmpty();
        verify(proyectoRepository).findAllByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Salta el proyecto que desapareció entre la lista y la carga")
    void armarShouldSkipProyectoThatCannotBeLoaded() {
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build()));
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThat(contextoNegocioService.armar(NEGOCIO_ID).inventarios()).isEmpty();
    }

    private void givenProyecto(Bloque... bloques) {
        Proyecto resumen = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        Proyecto completo = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).bloques(List.of(bloques)).build();
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(resumen));
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(completo));
    }
}
