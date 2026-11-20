package com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmarVerificacionWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-27T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final String TELEFONO = "5491122334455";
    private static final String CODIGO = "123456";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final VerificacionWhatsappRepository verificacionRepository =
            mock(VerificacionWhatsappRepository.class, CALLS_REAL_METHODS);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final ConfirmarVerificacionWhatsapp confirmar = new WhatsappUseCaseConfig()
            .confirmarVerificacionWhatsapp(negocioRepository, verificacionRepository,
                    new VinculacionWhatsappService(vinculoRepository, verificacionRepository), mensajeria,
                    new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC));
    private VerificacionWhatsapp verificacion;
    private ConfirmarVerificacionWhatsappResponse vinculacion;
    private Throwable error;

    @Test
    @DisplayName("Con el código correcto vincula el número y lo informa junto con el número del bot")
    void conElCodigoCorrectoVinculaElNumero() {
        dadoQueSeLeMandoUnCodigo();
        dadoQueElBotEs("5491155550000");

        cuandoConfirmaCon(CODIGO);

        entoncesQuedoVinculadoA(TELEFONO);
        entoncesElNumeroDelBotEs("5491155550000");
        entoncesSeContoElIntento();
    }

    @Test
    @DisplayName("Con el código correcto descarta el vínculo y la verificación anteriores y da la bienvenida")
    void conElCodigoCorrectoDescartaLoAnteriorYDaLaBienvenida() {
        dadoQueSeLeMandoUnCodigo();
        dadoQueElBotEs("5491155550000");

        cuandoConfirmaCon(CODIGO);

        entoncesSeDescartoLoAnterior();
        entoncesSeLeDioLaBienvenida();
    }

    @Test
    @DisplayName("Con un código incorrecto guarda el intento, no vincula y avisa cuántos quedan")
    void conUnCodigoIncorrectoGuardaElIntentoYNoVincula() {
        dadoQueSeLeMandoUnCodigo();

        cuandoIntentaConfirmarCon("000000");

        entoncesSeRechazaPorque("El código no es correcto. Te quedan 4 intentos");
        entoncesSeGuardoElIntento();
        entoncesNoSeVinculoNiSeMandoNada();
    }

    @Test
    @DisplayName("Sin haber pedido un código responde como inexistente")
    void sinHaberPedidoUnCodigoRespondeComoInexistente() {
        dadoQueNoPidioNingunCodigo();

        cuandoIntentaConfirmarCon(CODIGO);

        entoncesNoSeEncuentra();
    }

    private void dadoQueSeLeMandoUnCodigo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        verificacion = VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp(TELEFONO), CODIGO,
                AHORA.minusMinutes(1));
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(verificacion));
        when(vinculoRepository.save(any(VinculoWhatsapp.class))).thenAnswer(guardado -> guardado.getArgument(0));
    }

    private void dadoQueNoPidioNingunCodigo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoQueElBotEs(String numero) {
        when(mensajeria.numeroBot()).thenReturn(numero);
    }

    private void cuandoConfirmaCon(String codigo) {
        vinculacion = confirmar.execute(new ConfirmarVerificacionWhatsappRequest(USUARIO_ID, codigo));
    }

    private void cuandoIntentaConfirmarCon(String codigo) {
        error = catchThrowable(() -> cuandoConfirmaCon(codigo));
    }

    private void entoncesQuedoVinculadoA(String telefono) {
        assertThat(vinculacion.telefono()).isEqualTo(telefono);
    }

    private void entoncesElNumeroDelBotEs(String numero) {
        assertThat(vinculacion.numeroBot()).isEqualTo(numero);
    }

    private void entoncesSeContoElIntento() {
        assertThat(verificacion.getIntentos()).isEqualTo(1);
    }

    private void entoncesSeDescartoLoAnterior() {
        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
        verify(verificacionRepository).deleteByUsuarioId(USUARIO_ID);
    }

    private void entoncesSeLeDioLaBienvenida() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.BIENVENIDA);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesSeGuardoElIntento() {
        verify(verificacionRepository).save(verificacion);
    }

    private void entoncesNoSeVinculoNiSeMandoNada() {
        verify(vinculoRepository, never()).save(any());
        verify(mensajeria, never()).enviarTexto(any(), any());
    }

    private void entoncesNoSeEncuentra() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
    }
}