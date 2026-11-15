package com.ensambla.backend.domain.repository.marca;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KitDeMarcaRepositoryTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-15T10:00:00Z");

    private final KitDeMarcaRepository repository = mock(KitDeMarcaRepository.class, CALLS_REAL_METHODS);

    private KitDeMarca guardado;

    private KitDeMarca obtenido;

    @Test
    @DisplayName("Obtener devuelve el kit guardado del negocio")
    void obtenerOPredeterminadoShouldReturnStoredKit() {
        givenUnKitGuardado();

        whenSeObtieneElKit();

        assertThat(obtenido).isSameAs(guardado);
    }

    @Test
    @DisplayName("Obtener devuelve el kit predeterminado sin guardar nada si el negocio no armó el suyo")
    void obtenerOPredeterminadoShouldReturnDefaultWithoutSaving() {
        givenUnNegocioSinKit();

        whenSeObtieneElKit();

        assertThat(obtenido.getNegocioId()).isEqualTo(NEGOCIO_ID);
        thenNoSeGuardoNada();
    }

    private void givenUnKitGuardado() {
        guardado = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
        when(repository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(guardado));
    }

    private void givenUnNegocioSinKit() {
        when(repository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void whenSeObtieneElKit() {
        obtenido = repository.obtenerOPredeterminado(NEGOCIO_ID, AHORA);
    }

    private void thenNoSeGuardoNada() {
        verify(repository, never()).save(any());
    }
}
