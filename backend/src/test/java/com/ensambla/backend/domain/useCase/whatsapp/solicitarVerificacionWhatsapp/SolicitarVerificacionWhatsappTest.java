package com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitarVerificacionWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-27T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final VerificacionWhatsappRepository verificacionRepository =
            mock(VerificacionWhatsappRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final SolicitarVerificacionWhatsapp solicitar = new WhatsappUseCaseConfig()
            .solicitarVerificacionWhatsapp(negocioRepository, vinculoRepository, verificacionRepository, mensajeria,
                    new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("Manda por WhatsApp un código de 6 dígitos, guarda sólo su huella y no lo devuelve")
    void executeShouldSendCodeAndStoreOnlyItsHash() {
        givenNegocioDelUsuario();
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(true);
        when(verificacionRepository.save(any(VerificacionWhatsapp.class))).thenAnswer(i -> i.getArgument(0));

        SolicitarVerificacionWhatsappResponse result = solicitar.execute(request("+54 11 2233 4455"));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(mensajeria).enviarTexto(eq("5491122334455"), texto.capture());
        assertThat(texto.getValue()).containsPattern("es [0-9]{6}[.]");
        ArgumentCaptor<VerificacionWhatsapp> guardada = ArgumentCaptor.forClass(VerificacionWhatsapp.class);
        verify(verificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getCodigoHash()).hasSize(64);
        assertThat(result.telefono()).isEqualTo("5491122334455");
        assertThat(result.expiraEn()).isEqualTo(AHORA.plusMinutes(10));
        assertThat(result.reenviarDesde()).isEqualTo(AHORA.plusSeconds(60));
    }

    @Test
    @DisplayName("Si ya había una verificación en curso la reemplaza respetando la espera entre envíos")
    void executeShouldReplaceVerificationInProgress() {
        givenNegocioDelUsuario();
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(true);
        VerificacionWhatsapp previa = VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp("5491122334455"),
                "111111", AHORA.minusMinutes(2));
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(previa));
        when(verificacionRepository.save(any(VerificacionWhatsapp.class))).thenAnswer(i -> i.getArgument(0));

        solicitar.execute(request("5491100000000"));

        assertThat(previa.getTelefono()).isEqualTo("5491100000000");
        assertThat(previa.getEnviosVentana()).isEqualTo(2);
    }

    @Test
    @DisplayName("Pedir otro código antes de que pase la espera se rechaza y no manda nada")
    void executeShouldRejectResendTooSoon() {
        givenNegocioDelUsuario();
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(
                VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp("5491122334455"), "111111",
                        AHORA.minusSeconds(10))));

        assertThatThrownBy(() -> solicitar.execute(request("5491122334455")))
                .hasMessage("Esperá un momento antes de pedir otro código");

        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Un número que ya es de otra cuenta se rechaza con un conflicto")
    void executeShouldRejectNumberOfAnotherAccount() {
        givenNegocioDelUsuario();
        when(vinculoRepository.findByTelefono("5491122334455")).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(OTRO_USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp("5491122334455"), AHORA)));

        assertThatThrownBy(() -> solicitar.execute(request("5491122334455")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Pedir el código del número que ya tiene vinculado se rechaza")
    void executeShouldRejectOwnLinkedNumber() {
        givenNegocioDelUsuario();
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp("5491122334455"), AHORA)));

        assertThatThrownBy(() -> solicitar.execute(request("+54 9 11 2233 4455")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ese número ya está vinculado a tu cuenta");
    }

    @Test
    @DisplayName("A un mismo número no se le piden más de 3 códigos por hora entre todas las cuentas")
    void executeShouldLimitCodesPerPhone() {
        givenNegocioDelUsuario();
        when(verificacionRepository.contarEnviosRecientesA("5491122334455", AHORA.minusHours(1))).thenReturn(3L);

        assertThatThrownBy(() -> solicitar.execute(request("5491122334455")))
                .hasMessage("Ya mandamos varios códigos a ese número. Probá en una hora");

        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Si el código no se pudo entregar se descarta la verificación y se pide revisar el número")
    void executeShouldDiscardVerificationWhenSendFails() {
        givenNegocioDelUsuario();
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(false);
        when(verificacionRepository.save(any(VerificacionWhatsapp.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> solicitar.execute(request("5491122334455")))
                .hasMessageContaining("No pudimos mandarte el código por WhatsApp");

        verify(verificacionRepository).deleteByUsuarioId(USUARIO_ID);
    }

    @Test
    @DisplayName("Un teléfono con letras no es un número válido")
    void executeShouldRejectInvalidPhone() {
        givenNegocioDelUsuario();

        assertThatThrownBy(() -> solicitar.execute(request("no es un número")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void givenNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private static SolicitarVerificacionWhatsappRequest request(String telefono) {
        return new SolicitarVerificacionWhatsappRequest(USUARIO_ID, telefono);
    }
}
