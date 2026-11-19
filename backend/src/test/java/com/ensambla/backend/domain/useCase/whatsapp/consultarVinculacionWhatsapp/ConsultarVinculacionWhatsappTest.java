package com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarVinculacionWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final ConsultarVinculacionWhatsapp consultarVinculacionWhatsapp = new WhatsappUseCaseConfig()
            .consultarVinculacionWhatsapp(vinculoRepository, mensajeria);
    private ConsultarVinculacionWhatsappResponse vinculacion;
    private Throwable error;

    @Test
    @DisplayName("Consultar devuelve el número vinculado, cuándo se vinculó y el número del bot")
    void consultarDevuelveElNumeroVinculado() {
        dadoQueElNumeroDelBotEs("5491100000000");
        dadoQueElUsuarioVinculo("5491122334455");

        cuandoSeConsulta();

        entoncesEstaVinculadoA("5491122334455", AHORA);
        entoncesElNumeroDelBotEs("5491100000000");
    }

    @Test
    @DisplayName("Consultar sin número vinculado responde como inexistente")
    void consultarSinNumeroVinculadoRespondeComoInexistente() {
        dadoQueElUsuarioNoVinculoNingunNumero();

        cuandoSeIntentaConsultar();

        entoncesNoSeEncuentra();
    }

    private void dadoQueElNumeroDelBotEs(String numero) {
        when(mensajeria.numeroBot()).thenReturn(numero);
    }

    private void dadoQueElUsuarioVinculo(String telefono) {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(telefono), AHORA)));
    }

    private void dadoQueElUsuarioNoVinculoNingunNumero() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private void cuandoSeConsulta() {
        vinculacion = consultarVinculacionWhatsapp.execute(new ConsultarVinculacionWhatsappRequest(USUARIO_ID));
    }

    private void cuandoSeIntentaConsultar() {
        error = catchThrowable(this::cuandoSeConsulta);
    }

    private void entoncesEstaVinculadoA(String telefono, OffsetDateTime desde) {
        assertThat(vinculacion.telefono()).isEqualTo(telefono);
        assertThat(vinculacion.vinculadoEn()).isEqualTo(desde);
    }

    private void entoncesElNumeroDelBotEs(String numero) {
        assertThat(vinculacion.numeroBot()).isEqualTo(numero);
    }

    private void entoncesNoSeEncuentra() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
    }
}