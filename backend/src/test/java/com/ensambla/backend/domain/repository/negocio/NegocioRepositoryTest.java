package com.ensambla.backend.domain.repository.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class NegocioRepositoryTest {

    private static final String SIN_NEGOCIO = "No se encontró el negocio del usuario";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);

    private Negocio negocio;
    private NegocioId idObtenido;
    private Negocio negocioObtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Resuelve el negocio usando el usuario autenticado")
    void resuelveElNegocioUsandoElUsuarioAutenticado() {
        dadoQueElUsuarioTieneUnNegocio();

        cuandoSeObtieneElIdDelNegocioDelUsuario();

        entoncesSeResuelveElIdDelNegocio();
    }

    @Test
    @DisplayName("Responde no encontrado si el usuario no tiene negocio")
    void respondeNoEncontradoSiElUsuarioNoTieneNegocio() {
        dadoQueElUsuarioNoTieneNegocio();

        cuandoSeIntentaObtenerElIdDelNegocioDelUsuario();

        entoncesSeRespondeQueNoSeEncontroElNegocio();
    }

    @Test
    @DisplayName("Devuelve el negocio del usuario")
    void devuelveElNegocioDelUsuario() {
        dadoQueElUsuarioTieneUnNegocio();

        cuandoSeObtieneElNegocioDelUsuario();

        entoncesSeDevuelveElMismoNegocio();
    }

    @Test
    @DisplayName("Responde no encontrado al pedir el negocio de un usuario sin negocio")
    void respondeNoEncontradoAlPedirElNegocioDeUnUsuarioSinNegocio() {
        dadoQueElUsuarioNoTieneNegocio();

        cuandoSeIntentaObtenerElNegocioDelUsuario();

        entoncesSeRespondeQueNoSeEncontroElNegocio();
    }

    // --- dado ---

    private void dadoQueElUsuarioTieneUnNegocio() {
        negocio = Negocio.builder().id(NEGOCIO_ID).build();
        doReturn(Optional.of(NEGOCIO_ID)).when(negocioRepository).findIdByDuenioId(USUARIO_ID);
        doReturn(Optional.of(negocio)).when(negocioRepository).findByDuenioId(USUARIO_ID);
    }

    private void dadoQueElUsuarioNoTieneNegocio() {
        doReturn(Optional.empty()).when(negocioRepository).findIdByDuenioId(USUARIO_ID);
        doReturn(Optional.empty()).when(negocioRepository).findByDuenioId(USUARIO_ID);
    }

    // --- cuando ---

    private void cuandoSeObtieneElIdDelNegocioDelUsuario() {
        idObtenido = negocioRepository.obtenerIdDelUsuario(USUARIO_ID);
    }

    private void cuandoSeIntentaObtenerElIdDelNegocioDelUsuario() {
        rechazo = catchThrowable(this::cuandoSeObtieneElIdDelNegocioDelUsuario);
    }

    private void cuandoSeObtieneElNegocioDelUsuario() {
        negocioObtenido = negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID);
    }

    private void cuandoSeIntentaObtenerElNegocioDelUsuario() {
        rechazo = catchThrowable(this::cuandoSeObtieneElNegocioDelUsuario);
    }

    // --- entonces ---

    private void entoncesSeResuelveElIdDelNegocio() {
        assertThat(idObtenido).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesSeDevuelveElMismoNegocio() {
        assertThat(negocioObtenido).isSameAs(negocio);
    }

    private void entoncesSeRespondeQueNoSeEncontroElNegocio() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage(SIN_NEGOCIO);
    }
}
