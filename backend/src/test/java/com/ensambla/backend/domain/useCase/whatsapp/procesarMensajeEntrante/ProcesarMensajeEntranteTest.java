package com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante;

import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.infrastructure.whatsapp.WhatsappProperties;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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
    private static final String TELEFONO_CLIENTE = "5491199990000";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CodigoVinculacionRepository codigoRepository = mock(CodigoVinculacionRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final VerificacionWhatsappRepository verificacionRepository = mock(VerificacionWhatsappRepository.class);
    private final MensajeWhatsappRepository mensajeRepository = mock(MensajeWhatsappRepository.class);
    private final MensajeClienteRepository mensajeClienteRepository = mock(MensajeClienteRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final ProcesarMensajeEntrante procesarMensajeEntrante = new WhatsappUseCaseConfig()
            .procesarMensajeEntrante(
                    negocioRepository,
                    codigoRepository,
                    vinculoRepository,
                    mensajeRepository,
                    mensajeClienteRepository,
                    new VinculacionWhatsappService(vinculoRepository, verificacionRepository),
                    givenPropiedades(),
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
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.VINCULADO);
    }

    @Test
    @DisplayName("VINCULAR permite que el mismo usuario vuelva a vincular su número")
    void executeShouldAllowSameUserToRelink() {
        givenCodigo("ABC234", USUARIO_ID);
        givenNegocioDelUsuario();
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));

        procesarMensajeEntrante.execute(texto("ID1", "VINCULAR ABC234"));

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.VINCULADO);
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
    @DisplayName("Una foto de un número vinculado se guarda con su pie y su tipo")
    void executeShouldStorePhotoFromLinkedNumber() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));

        procesarMensajeEntrante.execute(new MensajeEntrante(TELEFONO, "ID5", TipoMensajeWhatsapp.IMAGEN, "ventas",
                "image/jpeg", null, AHORA));

        ArgumentCaptor<MensajeWhatsapp> captor = ArgumentCaptor.forClass(MensajeWhatsapp.class);
        verify(mensajeRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMensajeWhatsapp.IMAGEN);
        assertThat(captor.getValue().getTexto()).isEqualTo("ventas");
        assertThat(captor.getValue().getMimeMedia()).isEqualTo("image/jpeg");
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

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_VINCULADO);
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un texto de un cliente de la lista se guarda en la cola de clientes, no en la de comercios")
    void executeShouldStoreTextFromAllowedClientInItsOwnQueue() {
        when(vinculoRepository.findByTelefono(TELEFONO_CLIENTE)).thenReturn(Optional.empty());

        procesarMensajeEntrante.execute(deCliente("ID6", "dame 1 kilo de yerba"));

        ArgumentCaptor<MensajeCliente> captor = ArgumentCaptor.forClass(MensajeCliente.class);
        verify(mensajeClienteRepository).save(captor.capture());
        assertThat(captor.getValue().getTelefono()).isEqualTo(TELEFONO_CLIENTE);
        assertThat(captor.getValue().getTexto()).isEqualTo("dame 1 kilo de yerba");
        verify(mensajeRepository, never()).save(any());
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Un texto de un cliente que el proveedor reintentó no se guarda dos veces")
    void executeShouldIgnoreDuplicatedMessageFromClient() {
        when(vinculoRepository.findByTelefono(TELEFONO_CLIENTE)).thenReturn(Optional.empty());
        when(mensajeClienteRepository.existsByIdExterno("ID6")).thenReturn(true);

        procesarMensajeEntrante.execute(deCliente("ID6", "hola"));

        verify(mensajeClienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una foto o una nota de voz de un cliente recibe el aviso de que sólo entiendo texto y no se guarda")
    void executeShouldAskClientForText() {
        when(vinculoRepository.findByTelefono(TELEFONO_CLIENTE)).thenReturn(Optional.empty());

        procesarMensajeEntrante.execute(new MensajeEntrante(TELEFONO_CLIENTE, "ID7", TipoMensajeWhatsapp.AUDIO, null,
                "audio/ogg", 4, AHORA));

        verify(mensajeria).enviarTexto(TELEFONO_CLIENTE, TextosWhatsapp.CLIENTE_SOLO_TEXTO);
        verify(mensajeClienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un comercio vinculado sigue su camino aunque su número figure también en la lista de clientes")
    void executeShouldTreatLinkedNumberAsBusinessEvenIfListedAsClient() {
        when(vinculoRepository.findByTelefono(TELEFONO_CLIENTE)).thenReturn(Optional.of(VinculoWhatsapp.nuevo(
                USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO_CLIENTE), AHORA)));

        procesarMensajeEntrante.execute(deCliente("ID8", "gasté 5000 en harina"));

        verify(mensajeRepository).save(any(MensajeWhatsapp.class));
        verify(mensajeClienteRepository, never()).save(any());
    }

    private static WhatsappProperties givenPropiedades() {
        return new WhatsappProperties("simulado", "", "", new WhatsappProperties.Evolution("", "", "", Duration.ofSeconds(1)),
                new WhatsappProperties.Atencion(false, Duration.ofSeconds(1), Duration.ofMinutes(2), 0.6),
                new WhatsappProperties.Pedidos(List.of("+54 9 11 9999-0000", " ")));
    }

    private static MensajeEntrante deCliente(String idExterno, String texto) {
        return new MensajeEntrante(TELEFONO_CLIENTE, idExterno, TipoMensajeWhatsapp.TEXTO, texto, null, null, AHORA);
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
