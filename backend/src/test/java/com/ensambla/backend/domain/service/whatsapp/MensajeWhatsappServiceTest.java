package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.MEDIA_ID;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.TEXTO;
import static com.ensambla.backend.support.WhatsappTestData.WA_MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.audio;
import static com.ensambla.backend.support.WhatsappTestData.texto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MensajeWhatsappServiceTest {

    @Mock
    private MensajeWhatsappRepository mensajeWhatsappRepository;

    @Mock
    private VinculoWhatsappService vinculoWhatsappService;

    @InjectMocks
    private MensajeWhatsappService mensajeWhatsappService;

    @Test
    @DisplayName("Un texto de un número vinculado queda guardado en su negocio")
    void recibirShouldSaveTextInTheLinkedNegocio() {
        givenTelefonoVinculado();
        givenMensajeNuevo();

        mensajeWhatsappService.recibir(texto(TEXTO), AHORA);

        MensajeWhatsapp guardado = thenSaved();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getTipo()).isEqualTo(TipoMensajeWhatsapp.TEXTO);
        assertThat(guardado.getTexto()).isEqualTo(TEXTO);
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado queda guardada con el id del audio")
    void recibirShouldSaveAudioInTheLinkedNegocio() {
        givenTelefonoVinculado();
        givenMensajeNuevo();

        mensajeWhatsappService.recibir(audio(), AHORA);

        MensajeWhatsapp guardado = thenSaved();
        assertThat(guardado.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
        assertThat(guardado.getMediaId()).isEqualTo(MEDIA_ID);
    }

    @Test
    @DisplayName("Un número sin vincular no guarda nada en ningún negocio")
    void recibirShouldDiscardMessagesFromUnlinkedTelefono() {
        when(vinculoWhatsappService.negocioDelTelefono(TELEFONO)).thenReturn(Optional.empty());

        mensajeWhatsappService.recibir(texto(TEXTO), AHORA);

        verifyNoInteractions(mensajeWhatsappRepository);
    }

    @Test
    @DisplayName("Un código vigente vincula el número y no se guarda como mensaje")
    void recibirShouldLinkWithCodigoAndNotSaveIt() {
        when(vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA)).thenReturn(true);

        mensajeWhatsappService.recibir(texto("Ensambla " + CODIGO), AHORA);

        verify(vinculoWhatsappService, never()).negocioDelTelefono(TELEFONO);
        verifyNoInteractions(mensajeWhatsappRepository);
    }

    @Test
    @DisplayName("Seis números que no son un código vigente se tratan como un mensaje más")
    void recibirShouldSaveInvalidCodigoAsMessage() {
        when(vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA)).thenReturn(false);
        givenTelefonoVinculado();
        givenMensajeNuevo();

        mensajeWhatsappService.recibir(texto(CODIGO), AHORA);

        assertThat(thenSaved().getTexto()).isEqualTo(CODIGO);
    }

    @Test
    @DisplayName("Un mensaje que Meta avisa dos veces se guarda una sola vez")
    void recibirShouldIgnoreRepeatedMessage() {
        givenTelefonoVinculado();
        when(mensajeWhatsappRepository.existsByWaMensajeId(WA_MENSAJE_ID)).thenReturn(true);

        mensajeWhatsappService.recibir(texto(TEXTO), AHORA);

        verify(mensajeWhatsappRepository, never()).save(any());
    }

    // --- arrange ---
    private void givenTelefonoVinculado() {
        when(vinculoWhatsappService.negocioDelTelefono(TELEFONO)).thenReturn(Optional.of(NEGOCIO_ID));
    }

    private void givenMensajeNuevo() {
        when(mensajeWhatsappRepository.existsByWaMensajeId(WA_MENSAJE_ID)).thenReturn(false);
    }

    // --- assert ---
    private MensajeWhatsapp thenSaved() {
        ArgumentCaptor<MensajeWhatsapp> captor = ArgumentCaptor.forClass(MensajeWhatsapp.class);
        verify(mensajeWhatsappRepository).save(captor.capture());
        return captor.getValue();
    }
}
