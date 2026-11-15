package com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarRespuestasPendientesTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private ListarRespuestasPendientes listarRespuestasPendientes;
    private ListarRespuestasPendientesResponse respuesta;
    private Throwable error;

    @BeforeEach
    void setUp() {
        listarRespuestasPendientes = new ListarRespuestasPendientes(
                negocioRepository, objetivoRepository, miniObjetivoRepository);
    }

    @Test
    @DisplayName("Lista los manuales sin respuesta del negocio con el texto de su objetivo")
    void executeShouldListThePendingManualMinis() {
        givenUnManualSinResponder();

        whenSeListanLasRespuestasPendientes();

        thenSeListaElManualConElTextoDeSuObjetivo();
    }

    @Test
    @DisplayName("Sin pendientes devuelve una lista vacía")
    void executeShouldReturnAnEmptyList() {
        givenNingunManualSinResponder();

        whenSeListanLasRespuestasPendientes();

        thenNoHayPendientes();
    }

    @Test
    @DisplayName("Un usuario sin negocio responde como inexistente y no consulta nada más")
    void executeShouldFailWithoutBusiness() {
        givenUnUsuarioSinNegocio();

        whenSeListanLasRespuestasPendientes();

        thenRespondeComoInexistenteSinConsultarNadaMas();
    }

    // --- arrange ---
    private void givenUnManualSinResponder() {
        givenUnNegocioConObjetivo();
        when(miniObjetivoRepository.findAllPendientesDeRespuestaByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(miniManual()));
    }

    private void givenNingunManualSinResponder() {
        givenUnNegocioConObjetivo();
        when(miniObjetivoRepository.findAllPendientesDeRespuestaByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void givenUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    private void givenUnNegocioConObjetivo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo()));
    }

    // --- act ---
    private void whenSeListanLasRespuestasPendientes() {
        error = catchThrowable(() -> respuesta =
                listarRespuestasPendientes.execute(new ListarRespuestasPendientesRequest(USUARIO_ID)));
    }

    // --- assert ---
    private void thenSeListaElManualConElTextoDeSuObjetivo() {
        assertThat(respuesta.pendientes()).singleElement().satisfies(pendiente -> {
            assertThat(pendiente.objetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(pendiente.objetivoTexto()).isEqualTo(TEXTO);
            assertThat(pendiente.miniObjetivo().id()).isEqualTo(OTRO_MINI_ID);
        });
    }

    private void thenNoHayPendientes() {
        assertThat(respuesta.pendientes()).isEmpty();
    }

    private void thenRespondeComoInexistenteSinConsultarNadaMas() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(objetivoRepository, miniObjetivoRepository);
    }
}
