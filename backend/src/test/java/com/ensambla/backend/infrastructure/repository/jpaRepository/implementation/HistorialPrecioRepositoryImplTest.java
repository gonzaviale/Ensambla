package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.HistorialPrecioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IHistorialPrecioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.HistorialPrecioRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistorialPrecioRepositoryImplTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final IHistorialPrecioJpaRepository jpa = mock(IHistorialPrecioJpaRepository.class);
    private final HistorialPrecioRepositoryImpl repository =
            new HistorialPrecioRepositoryImpl(jpa, new HistorialPrecioRepositoryMapper());

    @Test
    @DisplayName("Guarda el cambio de precio y devuelve el que persistió")
    void saveShouldReturnPersistedChange() {
        when(jpa.saveAndFlush(any(HistorialPrecioEntity.class))).thenReturn(entity());

        CambioPrecio result = repository.save(cambio());

        assertThat(result).isEqualTo(cambio());
    }

    @Test
    @DisplayName("Lista los cambios de precio del negocio desde una fecha")
    void findByNegocioIdDesdeShouldMap() {
        when(jpa.findByNegocioIdAndCambiadoEnGreaterThanEqualOrderByCambiadoEnAsc(NEGOCIO_ID.valor(), AHORA.minusDays(30)))
                .thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioIdDesde(NEGOCIO_ID, AHORA.minusDays(30))).containsExactly(cambio());
    }

    private static CambioPrecio cambio() {
        return new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, new BigDecimal("1000"), new BigDecimal("1200"), AHORA);
    }

    private static HistorialPrecioEntity entity() {
        return HistorialPrecioEntity.builder().negocioId(NEGOCIO_ID.valor()).registroId(REGISTRO_ID.valor())
                .precioAnterior(new BigDecimal("1000")).precioNuevo(new BigDecimal("1200")).cambiadoEn(AHORA).build();
    }
}
