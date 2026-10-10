package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegocioRepositoryImplTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("Resuelve el id del negocio del dueño autenticado")
    void findIdByDuenioIdShouldMapId() {
        INegocioJpaRepository jpa = mock(INegocioJpaRepository.class);
        when(jpa.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID).build()));

        Optional<UUID> result = new NegocioRepositoryImpl(jpa).findIdByDuenioId(USUARIO_ID);

        assertThat(result).contains(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve vacío cuando el usuario no es dueño de ningún negocio")
    void findIdByDuenioIdShouldReturnEmptyWhenUsuarioOwnsNothing() {
        INegocioJpaRepository jpa = mock(INegocioJpaRepository.class);
        when(jpa.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID)).thenReturn(Optional.empty());

        Optional<UUID> result = new NegocioRepositoryImpl(jpa).findIdByDuenioId(USUARIO_ID);

        assertThat(result).isEmpty();
    }
}
