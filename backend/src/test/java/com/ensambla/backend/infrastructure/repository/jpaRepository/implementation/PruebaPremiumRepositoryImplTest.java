package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPruebaPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PruebaPremiumRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PruebaPremiumRepositoryImplTest {

    private final IPruebaPremiumJpaRepository pruebaPremiumJpaRepository = mock(IPruebaPremiumJpaRepository.class);
    private final PruebaPremiumRepositoryImpl pruebaPremiumRepository = new PruebaPremiumRepositoryImpl(
            pruebaPremiumJpaRepository, new PruebaPremiumRepositoryMapper());

    @Test
    @DisplayName("Recupera la prueba exclusivamente por el negocio solicitado")
    void findByNegocioIdShouldMapStoredTrial() {
        when(pruebaPremiumJpaRepository.findById(NEGOCIO_ID.valor())).thenReturn(Optional.of(givenEntity()));

        Optional<PruebaPremium> result = pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID);

        assertThat(result).contains(new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 4));
        verify(pruebaPremiumJpaRepository).findById(NEGOCIO_ID.valor());
    }

    @Test
    @DisplayName("Un negocio sin historial devuelve un resultado vacío")
    void findByNegocioIdShouldReturnEmptyWhenMissing() {
        when(pruebaPremiumJpaRepository.findById(NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        Optional<PruebaPremium> result = pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Guarda el período y su contador sin reemplazarlos por fechas actuales")
    void saveShouldMapStoredTrial() {
        when(pruebaPremiumJpaRepository.saveAndFlush(any())).thenReturn(givenEntity());

        PruebaPremium result = pruebaPremiumRepository.save(
                new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 4));

        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIa()).isEqualTo(4);
    }

    @Test
    @DisplayName("Delega la búsqueda de candidatos vencidos al instante solicitado")
    void findNegociosVencidosShouldDelegateInstant() {
        when(pruebaPremiumJpaRepository.findNegociosVencidos(INSTANTE)).thenReturn(List.of(NEGOCIO_ID.valor()));

        List<NegocioId> result = pruebaPremiumRepository.findNegociosVencidos(INSTANTE);

        assertThat(result).containsExactly(NEGOCIO_ID);
    }

    // --- arrange ---
    private PruebaPremiumEntity givenEntity() {
        return PruebaPremiumEntity.builder()
                .negocioId(NEGOCIO_ID.valor())
                .inicioEn(INSTANTE)
                .finEn(INSTANTE.plusDays(7))
                .paginasIa(4)
                .build();
    }
}
