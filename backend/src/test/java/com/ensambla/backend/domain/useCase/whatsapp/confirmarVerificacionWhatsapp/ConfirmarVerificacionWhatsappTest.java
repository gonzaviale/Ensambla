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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Con el código correcto vincula el número, descarta la verificación y da la bienvenida")
    void executeShouldLinkNumberWithRightCode() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");
        when(vinculoRepository.save(any(VinculoWhatsapp.class))).thenAnswer(i -> i.getArgument(0));
        when(mensajeria.numeroBot()).thenReturn("5491155550000");

        ConfirmarVerificacionWhatsappResponse result = confirmar.execute(request("123456"));

        assertThat(result.telefono()).isEqualTo(TELEFONO);
        assertThat(result.numeroBot()).isEqualTo("5491155550000");
        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
        verify(verificacionRepository).deleteByUsuarioId(USUARIO_ID);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.BIENVENIDA);
        assertThat(verificacion.getIntentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Con un código incorrecto guarda el intento, no vincula y avisa cuántos quedan")
    void executeShouldCountWrongAttemptAndNotLink() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");

        assertThatThrownBy(() -> confirmar.execute(request("000000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El código no es correcto. Te quedan 4 intentos");

        verify(verificacionRepository).save(verificacion);
        verify(vinculoRepository, never()).save(any());
        verify(mensajeria, never()).enviarTexto(any(), any());
    }

    @Test
    @DisplayName("Sin haber pedido un código responde como inexistente")
    void executeShouldFailWithoutVerification() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmar.execute(request("123456")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private VerificacionWhatsapp givenVerificacion(String codigo) {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        VerificacionWhatsapp verificacion = VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp(TELEFONO),
                codigo, AHORA.minusMinutes(1));
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(verificacion));
        return verificacion;
    }

    private static ConfirmarVerificacionWhatsappRequest request(String codigo) {
        return new ConfirmarVerificacionWhatsappRequest(USUARIO_ID, codigo);
    }
}
