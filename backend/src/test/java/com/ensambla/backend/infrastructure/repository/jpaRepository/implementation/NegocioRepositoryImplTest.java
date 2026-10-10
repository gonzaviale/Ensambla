package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.NegocioRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegocioRepositoryImplTest {

    private final INegocioJpaRepository negocioJpaRepository = mock(INegocioJpaRepository.class);
    private final NegocioRepositoryImpl negocioRepository = new NegocioRepositoryImpl(
            negocioJpaRepository, new NegocioRepositoryMapper());

    @Test
    @DisplayName("Devuelve el negocio con el id generado por persistencia")
    void saveShouldReturnPersistedNegocio() {
        when(negocioJpaRepository.saveAndFlush(any())).thenReturn(NegocioEntity.builder()
                .id(USUARIO_ID)
                .nombre("Mi negocio")
                .duenioId(USUARIO_ID)
                .build());

        Negocio result = negocioRepository.save(Negocio.builder().nombre("Mi negocio").build());

        assertThat(result.getId()).isEqualTo(USUARIO_ID);
        assertThat(result.getNombre()).isEqualTo("Mi negocio");
        assertThat(result.getDuenioId()).isEqualTo(USUARIO_ID);
    }
}

