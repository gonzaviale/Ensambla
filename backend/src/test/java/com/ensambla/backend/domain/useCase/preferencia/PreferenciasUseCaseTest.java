package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferenciasRequest;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferenciasRequest;
import com.ensambla.backend.infrastructure.config.useCase.PreferenciaUseCaseConfig;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciasUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(CLOCK);

    private final PreferenciaService preferenciaService = mock(PreferenciaService.class);
    private final PreferenciaUseCaseConfig config = new PreferenciaUseCaseConfig();
    private final ObtenerPreferencias obtenerPreferencias =
            config.obtenerPreferencias(preferenciaService, CLOCK);
    private final GuardarPreferencias guardarPreferencias =
            config.guardarPreferencias(preferenciaService, CLOCK);

    @Test
    @DisplayName("Obtener mapea las preferencias del usuario de sesión")
    void obtenerShouldMapPreferencias() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        preferencia.actualizar(List.of("a1"), Map.of("STOCK", false), true, "Gonza", AHORA);
        when(preferenciaService.obtener(USUARIO_ID, AHORA)).thenReturn(preferencia);

        PreferenciasItem result = obtenerPreferencias.execute(new ObtenerPreferenciasRequest(USUARIO_ID));

        assertThat(result).isEqualTo(
                new PreferenciasItem(List.of("a1"), Map.of("STOCK", false), true, "Gonza"));
    }

    @Test
    @DisplayName("Guardar delega con la hora del reloj y mapea lo persistido")
    void guardarShouldDelegateWithClock() {
        PreferenciaUsuario guardada = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        when(preferenciaService.guardar(USUARIO_ID, List.of("a1"), Map.of(), true, "Gonza", AHORA))
                .thenReturn(guardada);

        PreferenciasItem result = guardarPreferencias.execute(
                new GuardarPreferenciasRequest(USUARIO_ID, List.of("a1"), Map.of(), true, "Gonza"));

        assertThat(result.descartadas()).isEmpty();
        verify(preferenciaService).guardar(USUARIO_ID, List.of("a1"), Map.of(), true, "Gonza", AHORA);
    }
}
