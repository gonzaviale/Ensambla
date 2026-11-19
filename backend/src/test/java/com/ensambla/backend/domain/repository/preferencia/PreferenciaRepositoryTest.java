package com.ensambla.backend.domain.repository.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciaRepositoryTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final PreferenciaRepository preferenciaRepository =
            mock(PreferenciaRepository.class, CALLS_REAL_METHODS);

    private PreferenciaUsuario guardada;
    private PreferenciaUsuario obtenida;

    @Test
    @DisplayName("Obtener devuelve lo guardado")
    void obtenerDevuelveLoGuardado() {
        dadoQueElUsuarioYaGuardoPreferencias();

        cuandoSeObtienenLasPreferencias();

        entoncesSeDevuelvenLasGuardadas();
    }

    @Test
    @DisplayName("Obtener devuelve las iniciales sin escribir cuando nunca guardó nada")
    void obtenerDevuelveLasInicialesSinEscribirCuandoNuncaGuardoNada() {
        dadoQueElUsuarioNuncaGuardoPreferencias();

        cuandoSeObtienenLasPreferencias();

        entoncesSeDevuelvenLasInicialesSinGuardarNada();
    }

    // --- dado ---

    private void dadoQueElUsuarioYaGuardoPreferencias() {
        guardada = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(guardada));
    }

    private void dadoQueElUsuarioNuncaGuardoPreferencias() {
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtienenLasPreferencias() {
        obtenida = preferenciaRepository.obtenerOInicial(USUARIO_ID, AHORA);
    }

    // --- entonces ---

    private void entoncesSeDevuelvenLasGuardadas() {
        assertThat(obtenida).isSameAs(guardada);
    }

    private void entoncesSeDevuelvenLasInicialesSinGuardarNada() {
        assertThat(obtenida.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(obtenida.getDescartadas()).isEmpty();
        verify(preferenciaRepository, never()).save(any());
    }
}
