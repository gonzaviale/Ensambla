package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.dto.sistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.dto.sistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.dto.sistema.ListarSistemasRequest;
import com.ensambla.backend.domain.dto.sistema.SistemaItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import com.ensambla.backend.infrastructure.config.useCase.SistemaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SistemaUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID SISTEMA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final SistemaService sistemaService = mock(SistemaService.class);
    private final SistemaUseCaseConfig config = new SistemaUseCaseConfig();
    private final ListarSistemasUseCase listarSistemasUseCase =
            config.listarSistemasUseCase(negocioService, sistemaService, config.sistemaItemMapper());
    private final GuardarSistemaUseCase guardarSistemaUseCase =
            config.guardarSistemaUseCase(negocioService, sistemaService, config.sistemaItemMapper(), CLOCK);
    private final EliminarSistemaUseCase eliminarSistemaUseCase =
            config.eliminarSistemaUseCase(negocioService, sistemaService);

    @Test
    @DisplayName("Listar resuelve el negocio de la sesión y mapea los sistemas")
    void listarShouldMapSistemasOfSessionNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(sistemaService.listar(NEGOCIO_ID)).thenReturn(List.of(givenSistema()));

        List<SistemaItem> result = listarSistemasUseCase.execute(new ListarSistemasRequest(USUARIO_ID));

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(SISTEMA_ID.toString());
            assertThat(item.name()).isEqualTo("Mi almacén");
        });
    }

    @Test
    @DisplayName("Listar no consulta sistemas si el usuario no tiene negocio")
    void listarShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> listarSistemasUseCase.execute(new ListarSistemasRequest(USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(sistemaService);
    }

    @Test
    @DisplayName("Guardar arma el sistema con el negocio de la sesión y la hora del reloj y devuelve lo guardado")
    void guardarShouldBuildSistemaWithSessionNegocioAndClock() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(sistemaService.guardar(any(), any())).thenAnswer(call -> call.getArgument(1));

        SistemaItem result = guardarSistemaUseCase.execute(
                new GuardarSistemaRequest(USUARIO_ID, SISTEMA_ID.toString(), givenItem()));

        ArgumentCaptor<Sistema> captor = ArgumentCaptor.forClass(Sistema.class);
        verify(sistemaService).guardar(org.mockito.ArgumentMatchers.eq(NEGOCIO_ID), captor.capture());
        Sistema guardado = captor.getValue();
        assertThat(guardado.id()).isEqualTo(SISTEMA_ID);
        assertThat(guardado.proyecto().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.proyecto().getActualizadoEn()).isEqualTo(OffsetDateTime.parse("2026-10-21T10:00:00Z"));
        assertThat(result.id()).isEqualTo(SISTEMA_ID.toString());
    }

    @Test
    @DisplayName("Eliminar traduce el id de la pantalla y borra en el negocio de la sesión")
    void eliminarShouldDeleteInSessionNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);

        eliminarSistemaUseCase.execute(new EliminarSistemaRequest(USUARIO_ID, "e" + SISTEMA_ID));

        verify(sistemaService).eliminar(NEGOCIO_ID, SISTEMA_ID);
    }

    // --- arrange ---
    private Sistema givenSistema() {
        Proyecto proyecto = Proyecto.builder().id(SISTEMA_ID).negocioId(NEGOCIO_ID).nombre("Mi almacén")
                .creadoEn(OffsetDateTime.parse("2026-01-10T09:00:00Z"))
                .actualizadoEn(OffsetDateTime.parse("2026-10-21T10:00:00Z")).build();
        return new Sistema(proyecto, List.of(), Map.of());
    }

    private SistemaItem givenItem() {
        return new SistemaItem(SISTEMA_ID.toString(), "Mi almacén", null, null, List.of(
                new SeccionItem(UUID.randomUUID().toString(), "Vista general", List.<BloqueItem>of())),
                null, null, null, false);
    }
}
