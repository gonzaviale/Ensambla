package com.ensambla.backend.domain.repository.caja;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CobroQrRepositoryTest {

    private final CobroQrRepository cobroQrRepository = mock(CobroQrRepository.class, CALLS_REAL_METHODS);

    private CobroQr obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Obtiene el cobro con QR del negocio")
    void obtieneElCobroConQrDelNegocio() {
        dadoUnCobroGuardadoEnElNegocio();

        cuandoSeObtieneElCobro();

        entoncesSeObtieneElCobroGuardado();
    }

    @Test
    @DisplayName("Un cobro de otro negocio o inexistente responde no encontrado")
    void unCobroDeOtroNegocioOInexistenteRespondeNoEncontrado() {
        dadoQueElNegocioNoTieneEseCobro();

        cuandoSeIntentaObtenerElCobro();

        entoncesSeRespondeNoEncontrado();
    }

    // --- dado ---
    private void dadoUnCobroGuardadoEnElNegocio() {
        when(cobroQrRepository.findByIdAndNegocioId(COBRO_QR_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unCobroQrPendiente()));
    }

    private void dadoQueElNegocioNoTieneEseCobro() {
        when(cobroQrRepository.findByIdAndNegocioId(COBRO_QR_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeObtieneElCobro() {
        obtenido = cobroQrRepository.obtener(COBRO_QR_ID, NEGOCIO_ID);
    }

    private void cuandoSeIntentaObtenerElCobro() {
        rechazo = catchThrowable(this::cuandoSeObtieneElCobro);
    }

    // --- entonces ---
    private void entoncesSeObtieneElCobroGuardado() {
        assertThat(obtenido).isEqualTo(unCobroQrPendiente());
    }

    private void entoncesSeRespondeNoEncontrado() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Cobro con QR no encontrado");
    }
}
