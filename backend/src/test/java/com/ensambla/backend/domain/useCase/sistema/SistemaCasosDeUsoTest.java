package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemasRequest;
import com.ensambla.backend.infrastructure.config.useCase.SistemaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SistemaCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final ProyectoId SISTEMA_ID = ProyectoId.de("d0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(CLOCK);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final SistemaUseCaseConfig config = new SistemaUseCaseConfig();
    private final ListarSistemas listarSistemas = config.listarSistemas(negocioService, sistemaRepository);
    private final GuardarSistema guardarSistema = config.guardarSistema(negocioService, sistemaRepository,
            usoPlanRepository, controlPlanService, new TransaccionDirecta(), CLOCK);
    private final EliminarSistema eliminarSistema =
            config.eliminarSistema(negocioService, proyectoRepository, new TransaccionDirecta());

    @Test
    @DisplayName("Listar resuelve el negocio de la sesión y mapea los sistemas")
    void listarShouldMapSistemasOfSessionNegocio() {
        givenNegocio();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(givenSistema()));

        List<SistemaItem> result = listarSistemas.execute(new ListarSistemasRequest(USUARIO_ID));

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(SISTEMA_ID.toString());
            assertThat(item.name()).isEqualTo("Mi almacén");
        });
    }

    @Test
    @DisplayName("Listar no consulta sistemas si el usuario no tiene negocio")
    void listarShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> listarSistemas.execute(new ListarSistemasRequest(USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(sistemaRepository);
    }

    @Test
    @DisplayName("Guardar crea el sistema con el negocio de la sesión y la hora del reloj cuando el proyecto no existe")
    void guardarShouldCreateWithSessionNegocioAndClock() {
        givenNegocio();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.empty());

        SistemaItem result = guardarSistema.execute(givenGuardar());

        Sistema guardado = thenGuardado();
        assertThat(guardado.id()).isEqualTo(SISTEMA_ID);
        assertThat(guardado.proyecto().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.proyecto().getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.id()).isEqualTo(SISTEMA_ID.toString());
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        verify(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
    }

    @Test
    @DisplayName("Guardar reemplaza el sistema sin validar el alta cuando el proyecto ya es del negocio")
    void guardarShouldReplaceWhenOwned() {
        givenNegocio();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.of(NEGOCIO_ID));

        guardarSistema.execute(givenGuardar());

        thenGuardado();
        verify(controlPlanService, never()).validarAltaProyecto(any(), any());
    }

    @Test
    @DisplayName("Guardar responde como si no existiera cuando el proyecto es de otro negocio")
    void guardarShouldHideForeignProyecto() {
        givenNegocio();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.of(OTRO_NEGOCIO_ID));

        assertThatThrownBy(() -> guardarSistema.execute(givenGuardar()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sistema no encontrado");
        verify(sistemaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Guardar valida sólo el incremento de registros del sistema completo")
    void guardarShouldValidateOnlyAdditionalRows() {
        givenNegocio();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(usoPlanRepository.contarRegistrosProyecto(NEGOCIO_ID, SISTEMA_ID)).thenReturn(1L);

        guardarSistema.execute(givenGuardarConFilas(2));

        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
    }

    @Test
    @DisplayName("Guardar no valida registros cuando el reemplazo no suma filas")
    void guardarShouldNotValidateRowsWhenNoneAreAdded() {
        givenNegocio();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(usoPlanRepository.contarRegistrosProyecto(NEGOCIO_ID, SISTEMA_ID)).thenReturn(2L);

        guardarSistema.execute(givenGuardarConFilas(2));

        verify(controlPlanService, never()).validarAltaRegistros(any(), anyLong(), any());
    }

    @Test
    @DisplayName("Eliminar traduce el id de la pantalla y borra en el negocio de la sesión sin comprobar que exista")
    void eliminarShouldDeleteInSessionNegocio() {
        givenNegocio();

        eliminarSistema.execute(new EliminarSistemaRequest(USUARIO_ID, "e" + SISTEMA_ID));

        verify(proyectoRepository).deleteByIdAndNegocioId(SISTEMA_ID, NEGOCIO_ID);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Sistema givenSistema() {
        Proyecto proyecto = Proyecto.builder().id(SISTEMA_ID).negocioId(NEGOCIO_ID).nombre("Mi almacén")
                .creadoEn(OffsetDateTime.parse("2026-01-10T09:00:00Z"))
                .actualizadoEn(AHORA).build();
        return new Sistema(proyecto, List.of(), Map.of());
    }

    private GuardarSistemaRequest givenGuardar() {
        return new GuardarSistemaRequest(USUARIO_ID, SISTEMA_ID.toString(), new SistemaItem(
                SISTEMA_ID.toString(), "Mi almacén", null, null, List.of(
                        new SeccionItem(UUID.randomUUID().toString(), "Vista general", List.<BloqueItem>of())),
                null, null, null, false));
    }

    private GuardarSistemaRequest givenGuardarConFilas(int filas) {
        List<Map<String, Object>> filasDelBloque = IntStream.range(0, filas)
                .<Map<String, Object>>mapToObj(i -> Map.of("id", "fila-" + i))
                .toList();
        BloqueItem bloque = new BloqueItem("tabla", "table", "Productos", null, "blue", "half", List.of(),
                filasDelBloque, null, null, null);
        return new GuardarSistemaRequest(USUARIO_ID, SISTEMA_ID.toString(), new SistemaItem(
                SISTEMA_ID.toString(), "Mi almacén", null, null, List.of(
                        new SeccionItem("general", "Vista general", List.of(bloque))),
                null, null, null, false));
    }

    // --- assert ---
    private Sistema thenGuardado() {
        ArgumentCaptor<Sistema> captor = ArgumentCaptor.forClass(Sistema.class);
        verify(sistemaRepository).guardar(captor.capture());
        return captor.getValue();
    }
}
