package com.ensambla.backend.domain.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest.MensajeEntrante;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntranteWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.MEDIA_ID;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.RECIBIDO_EN;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.TEXTO;
import static com.ensambla.backend.support.WhatsappTestData.WA_MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.vinculoConCodigo;
import static org.assertj.core.api.Assertions.assertThat;

class WhatsappMappersTest {

    private final GenerarCodigoWhatsappMapper generarCodigoWhatsappMapper = new GenerarCodigoWhatsappMapper();
    private final RecibirMensajesWhatsappMapper recibirMensajesWhatsappMapper = new RecibirMensajesWhatsappMapper();

    @Test
    @DisplayName("Devuelve el código y su vencimiento")
    void generarCodigoToResponseShouldMapCodigoAndVencimiento() {
        CodigoWhatsappItem result = generarCodigoWhatsappMapper.toResponse(vinculoConCodigo(NEGOCIO_ID));

        assertThat(result.codigo()).isEqualTo(CODIGO);
        assertThat(result.venceEn()).isEqualTo(AHORA.plusMinutes(10));
    }

    @Test
    @DisplayName("Convierte cada mensaje con todos sus datos y en el mismo orden")
    void toEntrantesShouldMapEveryFieldInOrder() {
        RecibirMensajesWhatsappRequest request = new RecibirMensajesWhatsappRequest(List.of(
                new MensajeEntrante(WA_MENSAJE_ID, TELEFONO, "TEXTO", TEXTO, null, RECIBIDO_EN),
                new MensajeEntrante("wamid.2", TELEFONO, "AUDIO", null, MEDIA_ID, RECIBIDO_EN)));

        List<MensajeEntranteWhatsapp> result = recibirMensajesWhatsappMapper.toEntrantes(request);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).waMensajeId()).isEqualTo(WA_MENSAJE_ID);
        assertThat(result.get(0).telefono()).isEqualTo(TELEFONO);
        assertThat(result.get(0).tipo()).isEqualTo(TipoMensajeWhatsapp.TEXTO);
        assertThat(result.get(0).texto()).isEqualTo(TEXTO);
        assertThat(result.get(0).mediaId()).isNull();
        assertThat(result.get(0).recibidoEn()).isEqualTo(RECIBIDO_EN);
        assertThat(result.get(1).tipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
        assertThat(result.get(1).mediaId()).isEqualTo(MEDIA_ID);
    }

    @Test
    @DisplayName("Un aviso sin mensajes da una lista vacía")
    void toEntrantesShouldReturnEmptyList() {
        List<MensajeEntranteWhatsapp> result = recibirMensajesWhatsappMapper.toEntrantes(
                new RecibirMensajesWhatsappRequest(List.of()));

        assertThat(result).isEmpty();
    }
}
