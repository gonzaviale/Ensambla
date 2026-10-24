package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AvisoUseCasesTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID AVISO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(INSTANTE, ZoneOffset.UTC);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final AvisoService avisoService = mock(AvisoService.class);
    private final AccesoContenidoService accesoContenidoService = mock(AccesoContenidoService.class, invocation -> {
        if (invocation.getMethod().getName().equals("seleccionar")) {
            List<String> claves = invocation.getArgument(2);
            return new AccesoContenidoItem(claves, 0, null, claves.size(), 0, AHORA.plusMonths(1));
        }
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    });
    @Test
    @DisplayName("Listar devuelve los avisos del negocio del usuario filtrados por estado")
    void listarShouldReturnAvisosOfUsersNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoService.listar(NEGOCIO_ID, EstadoAviso.PENDIENTE)).thenReturn(List.of(givenAviso()));

        List<AvisoItem> result = new ListarAvisos(negocioService, avisoService, accesoContenidoService, CLOCK)
                .execute(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.PENDIENTE));

        assertThat(result).extracting(AvisoItem::id).containsExactly(AVISO_ID);
    }

    @Test
    @DisplayName("Contar devuelve los pendientes y urgentes del negocio del usuario")
    void contarShouldReturnCounterOfUsersNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoService.contar(NEGOCIO_ID)).thenReturn(new ContadorAvisos(4, 1));

        ContadorAvisos result = new ContarAvisos(negocioService, avisoService).execute(USUARIO_ID);

        assertThat(result).isEqualTo(new ContadorAvisos(4, 1));
    }

    @Test
    @DisplayName("Aplicar busca el aviso en el negocio del usuario y lo aplica con la hora del reloj")
    void aplicarShouldApplyAvisoOfUsersNegocio() {
        Aviso aviso = givenAviso();
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoService.obtener(AVISO_ID, NEGOCIO_ID)).thenReturn(aviso);
        when(avisoService.aplicar(aviso, AHORA)).thenReturn(aviso);

        AvisoItem result = new AplicarAviso(negocioService, avisoService, CLOCK, accesoContenidoService)
                .execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));

        assertThat(result.id()).isEqualTo(AVISO_ID);
        verify(avisoService).aplicar(aviso, AHORA);
    }

    @Test
    @DisplayName("Aplicar un aviso de otro negocio responde 'no existe' y no aplica nada")
    void aplicarShouldNotApplyForeignAviso() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoService.obtener(AVISO_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("El aviso no existe"));

        assertThatThrownBy(() -> new AplicarAviso(negocioService, avisoService, CLOCK, accesoContenidoService)
                .execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(avisoService).obtener(AVISO_ID, NEGOCIO_ID);
        verifyNoMoreInteractions(avisoService);
    }

    @Test
    @DisplayName("Descartar busca el aviso en el negocio del usuario y lo descarta con la hora del reloj")
    void descartarShouldDiscardAvisoOfUsersNegocio() {
        Aviso aviso = givenAviso();
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoService.obtener(AVISO_ID, NEGOCIO_ID)).thenReturn(aviso);
        when(avisoService.descartar(any(Aviso.class), any(OffsetDateTime.class))).thenReturn(aviso);

        AvisoItem result = new DescartarAviso(negocioService, avisoService, CLOCK, accesoContenidoService)
                .execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));

        assertThat(result.id()).isEqualTo(AVISO_ID);
        verify(avisoService).descartar(aviso, AHORA);
    }

    private static Aviso givenAviso() {
        return Aviso.builder().id(AVISO_ID).negocioId(NEGOCIO_ID).estado(EstadoAviso.PENDIENTE)
                .prioridad((short) 2).build();
    }
}
