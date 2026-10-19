package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.enums.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.MEDIA_ID;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.RECIBIDO_EN;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.TEXTO;
import static com.ensambla.backend.support.WhatsappTestData.WA_MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.audio;
import static com.ensambla.backend.support.WhatsappTestData.texto;
import static org.assertj.core.api.Assertions.assertThat;

class MensajeWhatsappTest {

    @Test
    @DisplayName("Un texto queda pendiente en el negocio con todos sus datos")
    void nuevoShouldKeepTheTextPending() {
        MensajeWhatsapp result = MensajeWhatsapp.nuevo(NEGOCIO_ID, texto(TEXTO), AHORA);

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTelefono()).isEqualTo(TELEFONO);
        assertThat(result.getWaMensajeId()).isEqualTo(WA_MENSAJE_ID);
        assertThat(result.getTipo()).isEqualTo(TipoMensajeWhatsapp.TEXTO);
        assertThat(result.getTexto()).isEqualTo(TEXTO);
        assertThat(result.getMediaId()).isNull();
        assertThat(result.getEstado()).isEqualTo(EstadoMensajeWhatsapp.PENDIENTE);
        assertThat(result.getRecibidoEn()).isEqualTo(RECIBIDO_EN);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Una nota de voz guarda el id del audio y no tiene texto")
    void nuevoShouldKeepTheAudioMediaId() {
        MensajeWhatsapp result = MensajeWhatsapp.nuevo(NEGOCIO_ID, audio(), AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
        assertThat(result.getMediaId()).isEqualTo(MEDIA_ID);
        assertThat(result.getTexto()).isNull();
    }
}
