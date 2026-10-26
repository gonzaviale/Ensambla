package com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcesarMensajeEntranteTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final String TELEFONO = "5491122334455";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CodigoVinculacionRepository codigoRepository = mock(CodigoVinculacionRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class);
    private final MensajeWhatsappRepository mensajeRepository = mock(MensajeWhatsappRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final ProcesarMensajeEntrante procesarMensajeEntrante = new WhatsappUseCaseConfig()
            .procesarMensajeEntrante(
                    negocioRepository,
                    codigoRepository,
                    vinculoRepository,
                    mensajeRepository,
                    mensajeria,
                    new TransaccionDirecta(),
                    Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("VINCULAR con un código vigente vincula el número al negocio del usuario y avisa por el chat")
    void executeShouldLinkNumberWithValidCodigo() {
        givenCodigo("ABC234", USUARIO_ID);
        givenNegocioDelUsuario();
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());

        procesarMensajeEntrante.execute(texto("ID1", " vincular abc234 "));

        VinculoWhatsapp vinculo = thenVinculoGuardado();
        assertThat(vinculo.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(vinculo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(vinculo.getTelefono()).isEqualTo(TELEFONO);
        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
        verify(codigoRepository).save(any(CodigoVinculacion.class));
        verify(mensajeria).enviarTexto(TELEFONO, ProcesarMensajeEntrante.RESPUESTA_VINCULADO);
    }

    @Test
    @DisplayName("VINCULAR permite que el mismo usuario vuelva a vincular su número")
    void executeShouldAllowSameUserToRelink() {
        givenCodigo("ABC234", USUARIO_ID);
        givenNegocioDelUsuario();
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));

        procesarMensajeEntrante.execute(texto("ID1", "VINCULAR ABC234"));

        verify(mensajeria).enviarTexto(TELEFONO, ProcesarMensajeEntrante.RESPUESTA_VINCULADO);
    }

    @Test
    @DisplayName("VINCULAR con un código inexistente responde el motivo y no vincula")
    void executeShouldReplyWhenCodigoDoesNotExist() {
        when(codigoRepository.findByCodigo(anyString())).thenReturn(Optional.empty());

        procesarMensajeEntrante.execute(texto("ID1", "VINCULAR ZZZ999"));

        verify(mensajeria).enviarTexto(TELEFONO, "El código de vinculación no existe");
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("VINCULAR con un código vencido responde el motivo y no vincula")
    void executeShouldReplyWhenCodigoExpired() {
        CodigoVinculacion vencido = CodigoVinculacion.nuevo(USUARIO_ID, "ABC234", AHORA.minusHours(1));
        when(codigoRepository.findByCodigo("ABC234")).thenReturn(Optional.of(vencido));

        procesarMensajeEntrante.execute(texto("ID1", "VINCULAR ABC234"));

        verify(mensajeria).enviarTexto(TELEFONO, "El código de vinculación venció o ya se usó");
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("VINCULAR un número que ya es de otra cuenta responde el motivo y no vincula")
    void executeShouldReplyWhenNumberBelongsToAnotherUser() {
        givenCodigo("ABC234", USUARIO_ID);
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(OTRO_USUARIO_ID)));

        procesarMensajeEntrante.execute(texto("ID1", "VINCULAR ABC234"));

        verify(mensajeria).enviarTexto(TELEFONO, "Ese número de WhatsApp ya está vinculado a otra cuenta");
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un texto de un número vinculado se guarda en el negocio del vínculo")
    void executeShouldStoreTextFromLinkedNumber() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));
        when(mensajeRepository.existsByIdExterno("ID2")).thenReturn(false);

        procesarMensajeEntrante.execute(texto("ID2", "gasté 5000 en harina"));

        ArgumentCaptor<MensajeWhatsapp> captor = ArgumentCaptor.forClass(MensajeWhatsapp.class);
        verify(mensajeRepository).save(captor.capture());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(captor.getValue().getTexto()).isEqualTo("gasté 5000 en harina");
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado se guarda sin buscar un código")
    void executeShouldStoreVoiceNoteFromLinkedNumber() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));

        procesarMensajeEntrante.execute(new MensajeEntrante(TELEFONO, "ID3", TipoMensajeWhatsapp.AUDIO, null,
                "audio/ogg", 4, AHORA));

        ArgumentCaptor<MensajeWhatsapp> captor = ArgumentCaptor.forClass(MensajeWhatsapp.class);
        verify(mensajeRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
    }

    @Test
    @DisplayName("Un mensaje que el proveedor reintentó no se guarda dos veces")
    void executeShouldIgnoreDuplicatedMessage() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));
        when(mensajeRepository.existsByIdExterno("ID2")).thenReturn(true);

        procesarMensajeEntrante.execute(texto("ID2", "hola"));

        verify(mensajeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un mensaje de un número sin vincular recibe las instrucciones y no se guarda")
    void executeShouldExplainHowToLinkUnknownNumber() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());

        procesarMensajeEntrante.execute(texto("ID4", "hola"));

        verify(mensajeria).enviarTexto(TELEFONO, ProcesarMensajeEntrante.RESPUESTA_NO_VINCULADO);
        verify(mensajeRepository, never()).save(any());
    }

    private void givenCodigo(String texto, UsuarioId usuarioId) {
        when(codigoRepository.findByCodigo(texto)).thenReturn(Optional.of(CodigoVinculacion.nuevo(usuarioId, texto, AHORA)));
    }

    private void givenNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private static VinculoWhatsapp givenVinculo(UsuarioId usuarioId) {
        return VinculoWhatsapp.nuevo(usuarioId, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA);
    }

    private static MensajeEntrante texto(String idExterno, String texto) {
        return new MensajeEntrante(TELEFONO, idExterno, TipoMensajeWhatsapp.TEXTO, texto, null, null, AHORA);
    }

    private VinculoWhatsapp thenVinculoGuardado() {
        ArgumentCaptor<VinculoWhatsapp> captor = ArgumentCaptor.forClass(VinculoWhatsapp.class);
        verify(vinculoRepository).save(captor.capture());
        return captor.getValue();
    }
}
