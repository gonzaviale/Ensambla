package com.ensambla.backend.domain.useCase.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerRequest;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem;
import com.ensambla.backend.domain.dto.sami.MensajeTallerItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.sami.ConversarEnTallerMapper;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sami.TallerSamiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversarEnTallerUseCaseTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID NEGOCIO_ID = UUID.randomUUID();
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final int LIMITE = 60;

    private final NegocioService negocioService = mock(NegocioService.class);
    private final TallerSamiService tallerSamiService = mock(TallerSamiService.class);
    private final ConversarEnTallerUseCase useCase = new ConversarEnTallerUseCase(
            negocioService, tallerSamiService, new ConversarEnTallerMapper(), Clock.fixed(INSTANTE, ZoneOffset.UTC), LIMITE);

    @Test
    @DisplayName("Resuelve el negocio de la sesión, conversa en el taller y devuelve el turno mapeado")
    void executeShouldResolveBusinessAndMapTheTurn() {
        OffsetDateTime ahora = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
        EstadoTaller estado = new EstadoTaller("Mi almacén", "mosaic", List.of());
        List<MensajeTaller> historial = List.of(new MensajeTaller("sami", "Hola"));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(tallerSamiService.conversar(NEGOCIO_ID, "sumá gastos", null, estado, historial, ahora, LIMITE))
                .thenReturn(new RespuestaTaller(
                        "Listo", List.of(new AccionTaller("agregar_pieza", Map.of("tipo", "expenses"))), null));

        ConversarEnTallerResponse result = useCase.execute(new ConversarEnTallerRequest(
                USUARIO_ID, "sumá gastos",
                new EstadoTallerItem("Mi almacén", "mosaic", List.of()),
                List.of(new MensajeTallerItem("sami", "Hola")), null));

        assertThat(result.respuesta()).isEqualTo("Listo");
        assertThat(result.acciones()).extracting("herramienta").containsExactly("agregar_pieza");
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no consulta a la IA")
    void executeShouldNotCallAiWhenBusinessIsMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> useCase.execute(new ConversarEnTallerRequest(
                USUARIO_ID, "hola", new EstadoTallerItem("", "mosaic", List.of()), List.of(), null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tallerSamiService, never()).conversar(any(), any(), any(), any(), any(), any(), anyInt());
    }
}
