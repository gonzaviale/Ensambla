package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.NegocioRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
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
                .id(USUARIO_ID.valor())
                .nombre("Mi negocio")
                .duenioId(USUARIO_ID.valor())
                .build());

        Negocio result = negocioRepository.save(Negocio.builder().nombre("Mi negocio").build());

        assertThat(result.getId()).isEqualTo(NegocioId.de(USUARIO_ID.valor()));
        assertThat(result.getNombre()).isEqualTo("Mi negocio");
        assertThat(result.getDuenioId()).isEqualTo(USUARIO_ID);
    }
    
    @Test
    @DisplayName("Resuelve el id del negocio del dueño autenticado")
    void findIdByDuenioIdShouldMapId() {
        var jpa = mock(INegocioJpaRepository.class);
        var mapper = new NegocioRepositoryMapper();
        when(jpa.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).build()));

        var result = new NegocioRepositoryImpl(jpa, mapper).findIdByDuenioId(USUARIO_ID);

        assertThat(result).contains(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista los ids de todos los negocios")
    void findAllIdsShouldDelegateToJpa() {
        when(negocioJpaRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID.valor()));

        assertThat(negocioRepository.findAllIds()).containsExactly(NEGOCIO_ID);
    }
}
