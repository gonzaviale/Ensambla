package com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesvincularWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final DesvincularWhatsapp desvincularWhatsapp = new WhatsappUseCaseConfig()
            .desvincularWhatsapp(vinculoRepository, new TransaccionDirecta());
    private Throwable error;

    @Test
    @DisplayName("Desvincular borra el número del usuario")
    void desvincularBorraElNumeroDelUsuario() {
        dadoQueElUsuarioTieneUnNumeroVinculado();

        cuandoSeDesvincula();

        entoncesSeBorroElVinculo();
    }

    @Test
    @DisplayName("Desvincular sin número vinculado responde como inexistente y no borra nada")
    void desvincularSinNumeroVinculadoNoBorraNada() {
        dadoQueElUsuarioNoTieneNumeroVinculado();

        cuandoSeIntentaDesvincular();

        entoncesNoSeEncuentra();
        entoncesNoSeBorroNada();
    }

    private void dadoQueElUsuarioTieneUnNumeroVinculado() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(VinculoWhatsapp.nuevo(
                USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp("5491122334455"), OffsetDateTime.now())));
    }

    private void dadoQueElUsuarioNoTieneNumeroVinculado() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private void cuandoSeDesvincula() {
        desvincularWhatsapp.execute(new DesvincularWhatsappRequest(USUARIO_ID));
    }

    private void cuandoSeIntentaDesvincular() {
        error = catchThrowable(this::cuandoSeDesvincula);
    }

    private void entoncesSeBorroElVinculo() {
        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
    }

    private void entoncesNoSeEncuentra() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeBorroNada() {
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
    }
}