package com.ensambla.backend.domain.useCase.objetivo.crearObjetivo;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.infrastructure.config.useCase.ObjetivoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearObjetivoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final CrearObjetivo crearObjetivo = new ObjetivoUseCaseConfig().crearObjetivo(
            negocioRepository,
            objetivoRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @BeforeEach
    void givenNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Guarda el objetivo en el negocio de la sesión con su texto, su meta y la fecha de hoy")
    void executeShouldSaveObjetivoOfSessionNegocio() {
        givenRepositorioAsignaId();

        whenCrear(" " + TEXTO + " ");

        Objetivo guardado = thenObjetivoGuardado();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getTexto()).isEqualTo(TEXTO);
        assertThat(guardado.getMeta()).isEqualTo(META);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Responde con el objetivo guardado, ya con su id")
    void executeShouldReturnSavedObjetivo() {
        givenRepositorioAsignaId();

        ObjetivoItem result = whenCrear(TEXTO);

        assertThat(result.id()).isEqualTo(OBJETIVO_ID);
        assertThat(result.texto()).isEqualTo(TEXTO);
        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Propaga el error del repositorio sin convertirlo")
    void executeShouldPropagateRepositoryError() {
        when(objetivoRepository.save(any())).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> whenCrear(TEXTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("base caída");
    }

    // --- arrange ---
    private void givenRepositorioAsignaId() {
        when(objetivoRepository.save(any())).thenAnswer(invocation -> {
            Objetivo objetivo = invocation.getArgument(0);
            objetivo.setId(OBJETIVO_ID);
            return objetivo;
        });
    }

    // --- act ---
    private ObjetivoItem whenCrear(String texto) {
        return crearObjetivo.execute(new CrearObjetivoRequest(USUARIO_ID, texto, META));
    }

    // --- assert ---
    private Objetivo thenObjetivoGuardado() {
        ArgumentCaptor<Objetivo> captor = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(captor.capture());
        return captor.getValue();
    }
}
