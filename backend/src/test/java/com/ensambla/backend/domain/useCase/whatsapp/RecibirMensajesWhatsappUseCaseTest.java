package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest.MensajeEntrante;
import com.ensambla.backend.domain.mapper.whatsapp.RecibirMensajesWhatsappMapper;
import com.ensambla.backend.domain.service.whatsapp.MensajeWhatsappService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.MEDIA_ID;
import static com.ensambla.backend.support.WhatsappTestData.RECIBIDO_EN;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.TEXTO;
import static com.ensambla.backend.support.WhatsappTestData.WA_MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.audio;
import static com.ensambla.backend.support.WhatsappTestData.texto;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RecibirMensajesWhatsappUseCaseTest {

    private final MensajeWhatsappService mensajeWhatsappService = mock(MensajeWhatsappService.class);

    private RecibirMensajesWhatsappUseCase recibirMensajesWhatsappUseCase;

    @BeforeEach
    void setUp() {
        recibirMensajesWhatsappUseCase = new RecibirMensajesWhatsappUseCase(
                mensajeWhatsappService,
                new RecibirMensajesWhatsappMapper(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Recibe cada mensaje del aviso, en orden y con la hora actual")
    void executeShouldReceiveEveryMessageInOrder() {
        RecibirMensajesWhatsappRequest request = new RecibirMensajesWhatsappRequest(List.of(
                new MensajeEntrante(WA_MENSAJE_ID, TELEFONO, "TEXTO", TEXTO, null, RECIBIDO_EN),
                new MensajeEntrante(WA_MENSAJE_ID, TELEFONO, "AUDIO", null, MEDIA_ID, RECIBIDO_EN)));

        recibirMensajesWhatsappUseCase.execute(request);

        InOrder orden = inOrder(mensajeWhatsappService);
        orden.verify(mensajeWhatsappService).recibir(texto(TEXTO), AHORA);
        orden.verify(mensajeWhatsappService).recibir(audio(), AHORA);
    }

    @Test
    @DisplayName("Un aviso sin mensajes no hace nada")
    void executeShouldDoNothingWithoutMessages() {
        recibirMensajesWhatsappUseCase.execute(new RecibirMensajesWhatsappRequest(List.of()));

        verifyNoInteractions(mensajeWhatsappService);
    }
}
