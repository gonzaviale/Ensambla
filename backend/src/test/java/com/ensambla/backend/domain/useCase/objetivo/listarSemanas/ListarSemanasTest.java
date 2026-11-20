package com.ensambla.backend.domain.useCase.objetivo.listarSemanas;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarSemanasTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final ResumenSemanalRepository resumenSemanalRepository = mock(ResumenSemanalRepository.class);
    private ListarSemanas listarSemanas;

    private ListarSemanasResponse respuesta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        listarSemanas = new ListarSemanas(negocioRepository, objetivoRepository, resumenSemanalRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve los resúmenes del objetivo del negocio de la sesión")
    void devuelveLosResumenesDelObjetivoDelNegocioDeLaSesion() {
        dadoUnObjetivoSinSemanasCerradas();

        cuandoSeListanLasSemanas();

        entoncesNoHayUltimoCierreNiSemanas();
    }

    @Test
    @DisplayName("Un objetivo ajeno responde como inexistente y no se leen sus resúmenes")
    void unObjetivoAjenoRespondeComoInexistenteYNoSeLeenSusResumenes() {
        dadoUnObjetivoAjeno();

        cuandoSeIntentaListarLasSemanas();

        entoncesSeRechazaComoInexistenteSinLeerResumenes();
    }

    // --- dado ---

    private void dadoUnObjetivoSinSemanasCerradas() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(resumenSemanalRepository.findAllByObjetivoId(OBJETIVO_ID)).thenReturn(List.of());
    }

    private void dadoUnObjetivoAjeno() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- cuando ---

    private void cuandoSeListanLasSemanas() {
        respuesta = listarSemanas.execute(new ListarSemanasRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeIntentaListarLasSemanas() {
        rechazo = catchThrowable(this::cuandoSeListanLasSemanas);
    }

    // --- entonces ---

    private void entoncesNoHayUltimoCierreNiSemanas() {
        assertThat(respuesta.ultimoCierre()).isNull();
        assertThat(respuesta.semanas()).isEmpty();
    }

    private void entoncesSeRechazaComoInexistenteSinLeerResumenes() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(resumenSemanalRepository);
    }
}
