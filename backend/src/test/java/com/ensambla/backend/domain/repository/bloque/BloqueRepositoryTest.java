package com.ensambla.backend.domain.repository.bloque;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BloqueRepositoryTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);

    private Bloque bloque;
    private Bloque obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Devuelve el bloque del negocio")
    void devuelveElBloqueDelNegocio() {
        dadoUnBloqueDelNegocio();

        cuandoSeObtieneElBloque();

        entoncesSeDevuelveElMismoBloque();
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el bloque es de otro negocio o no existe")
    void respondeNoExisteCuandoElBloqueEsDeOtroNegocioONoExiste() {
        dadoQueElBloqueNoExiste();

        cuandoSeIntentaObtenerElBloque();

        entoncesSeRespondeQueElBloqueNoExiste();
    }

    // --- dado ---

    private void dadoUnBloqueDelNegocio() {
        bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.of(bloque));
    }

    private void dadoQueElBloqueNoExiste() {
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtieneElBloque() {
        obtenido = bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeIntentaObtenerElBloque() {
        rechazo = catchThrowable(this::cuandoSeObtieneElBloque);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElMismoBloque() {
        assertThat(obtenido).isSameAs(bloque);
    }

    private void entoncesSeRespondeQueElBloqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("El bloque no existe");
    }
}
