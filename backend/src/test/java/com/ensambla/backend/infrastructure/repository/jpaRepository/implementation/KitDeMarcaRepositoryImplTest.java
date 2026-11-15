package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.KitDeMarcaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IKitDeMarcaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.KitDeMarcaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KitDeMarcaRepositoryImplTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-15T10:00:00Z");

    private final IKitDeMarcaJpaRepository jpa = mock(IKitDeMarcaJpaRepository.class);
    private final KitDeMarcaRepositoryImpl repository =
            new KitDeMarcaRepositoryImpl(jpa, new KitDeMarcaRepositoryMapper());

    @Test
    @DisplayName("Busca el kit por negocio y lo mapea al dominio")
    void findByNegocioIdShouldMapTheStoredKit() {
        givenUnKitGuardadoEnLaBase();

        Optional<KitDeMarca> result = repository.findByNegocioId(NEGOCIO_ID);

        assertThat(result).map(KitDeMarca::getNegocioId).contains(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Guarda el kit y devuelve lo persistido")
    void saveShouldReturnPersistedKit() {
        when(jpa.save(any(KitDeMarcaEntity.class))).thenReturn(entidad());

        KitDeMarca result = repository.save(KitDeMarca.predeterminado(NEGOCIO_ID, AHORA));

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void givenUnKitGuardadoEnLaBase() {
        when(jpa.findByNegocioId(NEGOCIO_ID.valor())).thenReturn(Optional.of(entidad()));
    }

    private KitDeMarcaEntity entidad() {
        KitDeMarca kit = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
        return new KitDeMarcaRepositoryMapper().toEntity(kit);
    }
}
