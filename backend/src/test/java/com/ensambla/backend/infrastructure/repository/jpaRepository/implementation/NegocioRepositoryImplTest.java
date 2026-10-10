package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegocioRepositoryImplTest {

    @Test
    @DisplayName("Resuelve el id del negocio del dueño autenticado")
    void findIdByDuenioIdShouldMapId() {
        var jpa = mock(INegocioJpaRepository.class);
        when(jpa.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID).build()));

        var result = new NegocioRepositoryImpl(jpa).findIdByDuenioId(USUARIO_ID);

        assertThat(result).contains(NEGOCIO_ID);
    }
}

