package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.PreferenciaUsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPreferenciaUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PreferenciaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreferenciaRepositoryImplTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final IPreferenciaUsuarioJpaRepository jpa = mock(IPreferenciaUsuarioJpaRepository.class);
    private final PreferenciaRepositoryImpl repository =
            new PreferenciaRepositoryImpl(jpa, new PreferenciaRepositoryMapper());

    private Optional<PreferenciaUsuario> encontrada;
    private PreferenciaUsuario guardada;

    @Test
    @DisplayName("Busca las preferencias por usuario y las mapea al dominio")
    void buscaLasPreferenciasPorUsuarioYLasMapeaAlDominio() {
        dadoQueElUsuarioTienePreferencias();

        cuandoSeBuscanLasPreferenciasDelUsuario();

        entoncesSeEncuentranLasPreferenciasDelUsuario();
    }

    @Test
    @DisplayName("Guarda las preferencias y devuelve lo persistido")
    void guardaLasPreferenciasYDevuelveLoPersistido() {
        dadoQueGuardarDevuelveLasPreferencias();

        cuandoSeGuardanLasPreferenciasIniciales();

        entoncesSeDevuelvenLasPreferenciasDelUsuario();
    }

    // --- dado ---
    private void dadoQueElUsuarioTienePreferencias() {
        when(jpa.findById(USUARIO_ID.valor())).thenReturn(Optional.of(unaEntidad()));
    }

    private void dadoQueGuardarDevuelveLasPreferencias() {
        when(jpa.save(any(PreferenciaUsuarioEntity.class))).thenReturn(unaEntidad());
    }

    private PreferenciaUsuarioEntity unaEntidad() {
        return PreferenciaUsuarioEntity.builder().usuarioId(USUARIO_ID.valor()).bienvenidaVista(false)
                .creadoEn(AHORA).actualizadoEn(AHORA).build();
    }

    // --- cuando ---
    private void cuandoSeBuscanLasPreferenciasDelUsuario() {
        encontrada = repository.findByUsuarioId(USUARIO_ID);
    }

    private void cuandoSeGuardanLasPreferenciasIniciales() {
        guardada = repository.save(PreferenciaUsuario.inicial(USUARIO_ID, AHORA));
    }

    // --- entonces ---
    private void entoncesSeEncuentranLasPreferenciasDelUsuario() {
        assertThat(encontrada).map(PreferenciaUsuario::getUsuarioId).contains(USUARIO_ID);
    }

    private void entoncesSeDevuelvenLasPreferenciasDelUsuario() {
        assertThat(guardada.getUsuarioId()).isEqualTo(USUARIO_ID);
    }
}
