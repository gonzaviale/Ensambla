package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciasCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(CLOCK);

    private final PreferenciaRepository preferenciaRepository = mock(PreferenciaRepository.class, CALLS_REAL_METHODS);
    private final PreferenciaUseCaseConfig config = new PreferenciaUseCaseConfig();
    private final ObtenerPreferencias obtenerPreferencias =
            config.obtenerPreferencias(preferenciaRepository, CLOCK);
    private final GuardarPreferencias guardarPreferencias = config.guardarPreferencias(
            preferenciaRepository, new TransaccionDirecta(), CLOCK);

    private PreferenciasItem resultado;

    @Test
    @DisplayName("Obtener mapea las preferencias del usuario de sesión")
    void obtenerMapeaLasPreferenciasDelUsuarioDeSesion() {
        dadoUnUsuarioConPreferenciasGuardadas();

        cuandoSeObtienenLasPreferencias();

        entoncesSeMapeanLasPreferenciasGuardadas();
    }

    @Test
    @DisplayName("Guardar actualiza las preferencias existentes y las persiste")
    void guardarActualizaLasPreferenciasExistentesYLasPersiste() {
        dadoUnUsuarioConPreferenciasDeAyer();
        dadoQueGuardarDevuelveLoGuardado();

        cuandoSeGuardanLasPreferenciasDeZona();

        entoncesSeActualizanYSePersisten();
    }

    @Test
    @DisplayName("Guardar crea las preferencias cuando todavía no existían")
    void guardarCreaLasPreferenciasCuandoTodaviaNoExistian() {
        dadoUnUsuarioSinPreferencias();
        dadoQueGuardarDevuelveLoGuardado();

        cuandoSeGuardanPreferenciasVacias();

        entoncesSeCreanVaciasYSePersisten();
    }

    // --- dado ---

    private void dadoUnUsuarioConPreferenciasGuardadas() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        preferencia.actualizar(List.of("a1"), Map.of("STOCK", false), true, "Gonza", AHORA);
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(preferencia));
    }

    private void dadoUnUsuarioConPreferenciasDeAyer() {
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID))
                .thenReturn(Optional.of(PreferenciaUsuario.inicial(USUARIO_ID, AHORA.minusDays(1))));
    }

    private void dadoUnUsuarioSinPreferencias() {
        when(preferenciaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoQueGuardarDevuelveLoGuardado() {
        when(preferenciaRepository.save(any(PreferenciaUsuario.class))).thenAnswer(call -> call.getArgument(0));
    }

    // --- cuando ---

    private void cuandoSeObtienenLasPreferencias() {
        resultado = obtenerPreferencias.execute(new ObtenerPreferenciasRequest(USUARIO_ID));
    }

    private void cuandoSeGuardanLasPreferenciasDeZona() {
        resultado = guardarPreferencias.execute(
                new GuardarPreferenciasRequest(USUARIO_ID, List.of("a1"), Map.of("ZONA", false), true, "Gonza"));
    }

    private void cuandoSeGuardanPreferenciasVacias() {
        resultado = guardarPreferencias.execute(
                new GuardarPreferenciasRequest(USUARIO_ID, List.of(), Map.of(), false, null));
    }

    // --- entonces ---

    private void entoncesSeMapeanLasPreferenciasGuardadas() {
        assertThat(resultado).isEqualTo(
                new PreferenciasItem(List.of("a1"), Map.of("STOCK", false), true, "Gonza"));
    }

    private void entoncesSeActualizanYSePersisten() {
        assertThat(resultado).isEqualTo(new PreferenciasItem(List.of("a1"), Map.of("ZONA", false), true, "Gonza"));
        verify(preferenciaRepository).save(any(PreferenciaUsuario.class));
    }

    private void entoncesSeCreanVaciasYSePersisten() {
        assertThat(resultado.descartadas()).isEmpty();
        verify(preferenciaRepository).save(any(PreferenciaUsuario.class));
    }
}
