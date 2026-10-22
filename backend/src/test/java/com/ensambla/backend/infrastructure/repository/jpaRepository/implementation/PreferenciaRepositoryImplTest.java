package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.infrastructure.repository.entity.PreferenciaUsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPreferenciaUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PreferenciaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreferenciaRepositoryImplTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final IPreferenciaUsuarioJpaRepository jpa = mock(IPreferenciaUsuarioJpaRepository.class);
    private final PreferenciaRepositoryImpl repository =
            new PreferenciaRepositoryImpl(jpa, new PreferenciaRepositoryMapper());

    @Test
    @DisplayName("Busca las preferencias por usuario y las mapea al dominio")
    void findByUsuarioIdShouldMap() {
        when(jpa.findById(USUARIO_ID)).thenReturn(Optional.of(entity()));

        assertThat(repository.findByUsuarioId(USUARIO_ID)).map(PreferenciaUsuario::getUsuarioId).contains(USUARIO_ID);
    }

    @Test
    @DisplayName("Guarda las preferencias y devuelve lo persistido")
    void saveShouldReturnPersisted() {
        when(jpa.save(any(PreferenciaUsuarioEntity.class))).thenReturn(entity());

        PreferenciaUsuario result = repository.save(PreferenciaUsuario.inicial(USUARIO_ID, AHORA));

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
    }

    private PreferenciaUsuarioEntity entity() {
        return PreferenciaUsuarioEntity.builder().usuarioId(USUARIO_ID).bienvenidaVista(false)
                .creadoEn(AHORA).actualizadoEn(AHORA).build();
    }
}
