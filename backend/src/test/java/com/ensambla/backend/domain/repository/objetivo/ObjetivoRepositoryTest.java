package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObjetivoRepositoryTest {

    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class, CALLS_REAL_METHODS);

    private Objetivo obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Obtiene un objetivo que pertenece al negocio")
    void obtieneUnObjetivoQuePerteneceAlNegocio() {
        dadoUnObjetivoDelNegocio();

        cuandoSeObtiene();

        entoncesSeObtieneElObjetivo();
    }

    @Test
    @DisplayName("Un objetivo inexistente o ajeno produce el mismo 404")
    void unObjetivoInexistenteOAjenoProduceElMismo404() {
        dadoNingunObjetivoDelNegocio();

        cuandoSeIntentaObtener();

        entoncesSeRechazaComoInexistente();
    }

    // --- dado ---

    private void dadoUnObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.of(objetivo()));
    }

    private void dadoNingunObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtiene() {
        obtenido = objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID);
    }

    private void cuandoSeIntentaObtener() {
        rechazo = catchThrowable(this::cuandoSeObtiene);
    }

    // --- entonces ---

    private void entoncesSeObtieneElObjetivo() {
        assertThat(obtenido.getId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeRechazaComoInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Objetivo no encontrado");
    }
}
