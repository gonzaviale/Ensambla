package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoVinculacionItem;
import com.ensambla.backend.domain.dto.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.dto.whatsapp.VinculoWhatsappItem;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.whatsapp.WhatsappItemMapper;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.RecepcionWhatsappService;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsappUseCasesTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final String TELEFONO = "5491122334455";
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(INSTANTE, ZoneOffset.UTC);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final VinculacionWhatsappService vinculacionService = mock(VinculacionWhatsappService.class);
    private final RecepcionWhatsappService recepcionService = mock(RecepcionWhatsappService.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final WhatsappItemMapper mapper = new WhatsappItemMapper();

    @Test
    @DisplayName("Generar el código devuelve el código, cuándo vence y el número del bot")
    void generarCodigoShouldReturnCodeWithBotNumber() {
        when(vinculacionService.generarCodigo(USUARIO_ID, AHORA))
                .thenReturn(CodigoVinculacion.nuevo(USUARIO_ID, "ABC234", AHORA));
        when(mensajeria.numeroBot()).thenReturn("5491100000000");

        CodigoVinculacionItem result = new GenerarCodigoVinculacionUseCase(negocioService, vinculacionService,
                mensajeria, mapper, CLOCK).execute(USUARIO_ID);

        assertThat(result).isEqualTo(new CodigoVinculacionItem("ABC234", AHORA.plusMinutes(10), "5491100000000"));
        verify(negocioService).obtenerDelUsuario(USUARIO_ID);
    }

    @Test
    @DisplayName("Un usuario sin negocio no puede generar el código")
    void generarCodigoShouldFailWhenUserHasNoNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("sin negocio"));

        assertThatThrownBy(() -> new GenerarCodigoVinculacionUseCase(negocioService, vinculacionService, mensajeria,
                mapper, CLOCK).execute(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
        verify(vinculacionService, never()).generarCodigo(any(), any());
    }

    @Test
    @DisplayName("Consultar devuelve el número vinculado del usuario")
    void consultarShouldReturnUsersLink() {
        when(vinculacionService.obtener(USUARIO_ID)).thenReturn(givenVinculo());

        VinculoWhatsappItem result = new ConsultarVinculacionWhatsappUseCase(vinculacionService, mapper)
                .execute(USUARIO_ID);

        assertThat(result).isEqualTo(new VinculoWhatsappItem(TELEFONO, AHORA));
    }

    @Test
    @DisplayName("Desvincular borra el número del usuario")
    void desvincularShouldDeleteUsersLink() {
        new DesvincularWhatsappUseCase(vinculacionService).execute(USUARIO_ID);

        verify(vinculacionService).desvincular(USUARIO_ID);
    }

    @Test
    @DisplayName("VINCULAR con un código válido vincula el número y avisa por el chat")
    void procesarShouldLinkNumberWhenCommandHasValidCode() {
        procesar(texto("vincular abc234"));

        verify(vinculacionService).canjear("abc234", new TelefonoWhatsapp(TELEFONO), AHORA);
        verify(mensajeria).enviarTexto(TELEFONO, ProcesarMensajeEntranteUseCase.RESPUESTA_VINCULADO);
        verify(recepcionService, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("VINCULAR con un código inválido responde el motivo y no guarda nada")
    void procesarShouldReplyReasonWhenCodeIsInvalid() {
        when(vinculacionService.canjear(anyString(), any(), any()))
                .thenThrow(new BusinessRuleException("El código de vinculación venció o ya se usó"));

        procesar(texto("VINCULAR ABC234"));

        verify(mensajeria).enviarTexto(TELEFONO, "El código de vinculación venció o ya se usó");
        verify(recepcionService, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("Un texto de un número vinculado se guarda y no se responde")
    void procesarShouldStoreTextFromLinkedNumber() {
        VinculoWhatsapp vinculo = givenVinculo();
        when(vinculacionService.buscarPorTelefono(new TelefonoWhatsapp(TELEFONO))).thenReturn(Optional.of(vinculo));
        MensajeEntrante entrante = texto("gasté 5000 en harina");

        procesar(entrante);

        verify(recepcionService).registrar(vinculo, entrante);
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado se guarda aunque su texto sea nulo")
    void procesarShouldStoreVoiceNoteFromLinkedNumber() {
        VinculoWhatsapp vinculo = givenVinculo();
        when(vinculacionService.buscarPorTelefono(new TelefonoWhatsapp(TELEFONO))).thenReturn(Optional.of(vinculo));
        MensajeEntrante entrante = new MensajeEntrante(TELEFONO, "ID2", TipoMensajeWhatsapp.AUDIO, null, "audio/ogg",
                5, AHORA);

        procesar(entrante);

        verify(recepcionService).registrar(vinculo, entrante);
    }

    @Test
    @DisplayName("Un número sin vincular no guarda nada y recibe el aviso")
    void procesarShouldRejectUnlinkedNumber() {
        when(vinculacionService.buscarPorTelefono(new TelefonoWhatsapp(TELEFONO))).thenReturn(Optional.empty());

        procesar(texto("gasté 5000 en harina"));

        verify(mensajeria).enviarTexto(TELEFONO, ProcesarMensajeEntranteUseCase.RESPUESTA_NO_VINCULADO);
        verify(recepcionService, never()).registrar(any(), any());
    }

    private void procesar(MensajeEntrante entrante) {
        new ProcesarMensajeEntranteUseCase(vinculacionService, recepcionService, mensajeria, CLOCK)
                .execute(entrante);
    }

    private static MensajeEntrante texto(String contenido) {
        return new MensajeEntrante(TELEFONO, "ID1", TipoMensajeWhatsapp.TEXTO, contenido, null, null, AHORA);
    }

    private static VinculoWhatsapp givenVinculo() {
        return VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA);
    }
}
