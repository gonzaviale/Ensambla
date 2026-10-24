package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferenciasRequest;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferenciasRequest;
import com.ensambla.backend.infrastructure.config.useCase.PreferenciaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciasCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(CLOCK);

    private final PreferenciaRepository preferenciaRepository = mock(PreferenciaRepository.class);
    private final PreferenciaService preferenciaService = new PreferenciaService(preferenciaRepository);
    private final PreferenciaUseCaseConfig config = new PreferenciaUseCaseConfig();
    private final ObtenerPreferencias obtenerPreferencias =
            config.obtenerPreferencias(preferenciaService, CLOCK);
    private final GuardarPreferencias guardarPreferencias = config.guardarPreferencias(
            preferenciaService, preferenciaRepository, new TransaccionDirecta(), CLOCK);

    @Test
    @DisplayName("Obtener mapea las preferencias del usuario de sesión")
    void obtenerShouldMapPreferencias() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        preferencia.actualizar(List.of("a1"), Map.of("STOCK", false), true, "Gonza", AHORA);
        givenGuardada(preferencia);

        PreferenciasItem result = obtenerPreferencias.execute(new ObtenerPreferenciasRequest(USUARIO_ID));

        assertThat(result).isEqualTo(
                new PreferenciasItem(List.of("a1"), Map.of("STOCK", false), true, "Gonza"));
    }

    @Test
    @DisplayName("Guardar actualiza las preferencias existentes y las persiste")
    void guardarShouldUpdateAndSave() {
        givenGuardada(PreferenciaUsuario.inicial(USUARIO_ID, AHORA.minusDays(1)));
        givenSaveDevuelveLoGuardado();

        PreferenciasItem result = guardarPreferencias.execute(
                new GuardarPreferenciasRequest(USUARIO_ID, List.of("a1"), Map.of("ZONA", false), true, "Gonza"));

        assertThat(result).isEqualTo(new PreferenciasItem(List.of("a1"), Map.of("ZONA", false), true, "Gonza"));
        verify(preferenciaRepository).save(any(PreferenciaUsuario.class));
    }

    @Test
    @DisplayName("Guardar crea las preferencias cuando todavía no existían")
    void guardarShouldCreateWhenMissing() {
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
        givenSaveDevuelveLoGuardado();

        PreferenciasItem result = guardarPreferencias.execute(
                new GuardarPreferenciasRequest(USUARIO_ID, List.of(), Map.of(), false, null));

        assertThat(result.descartadas()).isEmpty();
        verify(preferenciaRepository).save(any(PreferenciaUsuario.class));
    }

    // --- arrange ---
    private void givenGuardada(PreferenciaUsuario preferencia) {
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(preferencia));
    }

    private void givenSaveDevuelveLoGuardado() {
        when(preferenciaRepository.save(any(PreferenciaUsuario.class))).thenAnswer(call -> call.getArgument(0));
    }
}
