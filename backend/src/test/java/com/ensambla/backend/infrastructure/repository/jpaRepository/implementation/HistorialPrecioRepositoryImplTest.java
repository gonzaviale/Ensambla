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

    private CambioPrecio guardado;
    private List<CambioPrecio> cambios;

    @Test
    @DisplayName("Guarda el cambio de precio y devuelve el que persistió")
    void guardaElCambioDePrecioYDevuelveElQuePersistio() {
        dadoQueGuardarDevuelveElCambio();

        cuandoSeGuardaElCambioDePrecio();

        entoncesSeDevuelveElCambioPersistido();
    }

    @Test
    @DisplayName("Lista los cambios de precio del negocio desde una fecha")
    void listaLosCambiosDePrecioDelNegocioDesdeUnaFecha() {
        dadoQueElNegocioTieneUnCambioReciente();

        cuandoSeListanLosCambiosDeLosUltimosTreintaDias();

        entoncesSeListaElCambioDePrecio();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveElCambio() {
        when(jpa.saveAndFlush(any(HistorialPrecioEntity.class))).thenReturn(unaEntidad());
    }

    private void dadoQueElNegocioTieneUnCambioReciente() {
        when(jpa.findByNegocioIdAndCambiadoEnGreaterThanEqualOrderByCambiadoEnAsc(
                NEGOCIO_ID.valor(), AHORA.minusDays(30))).thenReturn(List.of(unaEntidad()));
    }

    private static CambioPrecio unCambio() {
        return new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, new BigDecimal("1000"), new BigDecimal("1200"), AHORA);
    }

    private static HistorialPrecioEntity unaEntidad() {
        return HistorialPrecioEntity.builder().negocioId(NEGOCIO_ID.valor()).registroId(REGISTRO_ID.valor())
                .precioAnterior(new BigDecimal("1000")).precioNuevo(new BigDecimal("1200")).cambiadoEn(AHORA).build();
    }

    // --- cuando ---
    private void cuandoSeGuardaElCambioDePrecio() {
        guardado = repository.save(unCambio());
    }

    private void cuandoSeListanLosCambiosDeLosUltimosTreintaDias() {
        cambios = repository.findByNegocioIdDesde(NEGOCIO_ID, AHORA.minusDays(30));
    }

    // --- entonces ---
    private void entoncesSeDevuelveElCambioPersistido() {
        assertThat(guardado).isEqualTo(unCambio());
    }

    private void entoncesSeListaElCambioDePrecio() {
        assertThat(cambios).containsExactly(unCambio());
    }
}
