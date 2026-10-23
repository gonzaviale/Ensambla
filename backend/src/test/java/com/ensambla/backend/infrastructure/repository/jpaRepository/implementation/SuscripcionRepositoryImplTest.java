package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISuscripcionJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.SuscripcionRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static com.ensambla.backend.support.PlanTestData.suscripcionEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuscripcionRepositoryImplTest {

    private final ISuscripcionJpaRepository suscripcionJpaRepository = mock(ISuscripcionJpaRepository.class);
    private final SuscripcionRepositoryImpl suscripcionRepository = new SuscripcionRepositoryImpl(
            suscripcionJpaRepository, new SuscripcionRepositoryMapper());

    @Test
    @DisplayName("Recupera y mapea suscripcion desde persistencia")
    void findByNegocioIdShouldMapStoredEntity() {
        when(suscripcionJpaRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcionEntity()));

        Optional<Suscripcion> result = suscripcionRepository.findByNegocioId(NEGOCIO_ID);

        assertThat(result).isPresent();
        thenEveryFieldMatches(result.orElseThrow());
        verify(suscripcionJpaRepository).findByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Conserva el resultado vacío cuando no existe suscripcion")
    void findByNegocioIdShouldReturnEmptyWhenMissing() {
        when(suscripcionJpaRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        Optional<Suscripcion> result = suscripcionRepository.findByNegocioId(NEGOCIO_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Guarda todos los campos y devuelve la suscripción persistida")
    void saveShouldMapBothDirections() {
        when(suscripcionJpaRepository.saveAndFlush(refEq(suscripcionEntity())))
                .thenReturn(suscripcionEntity());

        Suscripcion result = suscripcionRepository.save(suscripcion());

        thenEveryFieldMatches(result);
        verify(suscripcionJpaRepository).saveAndFlush(refEq(suscripcionEntity()));
    }
    // --- assert ---
    private void thenEveryFieldMatches(Suscripcion result) {
        Suscripcion esperado = suscripcion();
        assertThat(result.getId()).isEqualTo(esperado.getId());
        assertThat(result.getNegocioId()).isEqualTo(esperado.getNegocioId());
        assertThat(result.getPlanId()).isEqualTo(esperado.getPlanId());
        assertThat(result.getEstado()).isEqualTo(esperado.getEstado());
        assertThat(result.getInicioEn()).isEqualTo(esperado.getInicioEn());
        assertThat(result.getFinEn()).isEqualTo(esperado.getFinEn());
        assertThat(result.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }
}
