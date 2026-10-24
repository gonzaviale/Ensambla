package com.ensambla.backend.domain.service.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciaServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final PreferenciaRepository preferenciaRepository = mock(PreferenciaRepository.class);
    private final PreferenciaService preferenciaService = new PreferenciaService(preferenciaRepository);

    @Test
    @DisplayName("Obtener devuelve lo guardado")
    void obtenerShouldReturnStoredPreferencias() {
        PreferenciaUsuario guardada = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(guardada));

        assertThat(preferenciaService.obtener(USUARIO_ID, AHORA)).isSameAs(guardada);
    }

    @Test
    @DisplayName("Obtener devuelve las iniciales sin escribir cuando nunca guardó nada")
    void obtenerShouldReturnInitialWithoutSaving() {
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        PreferenciaUsuario result = preferenciaService.obtener(USUARIO_ID, AHORA);

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.getDescartadas()).isEmpty();
        verify(preferenciaRepository, never()).save(any());
    }
}
