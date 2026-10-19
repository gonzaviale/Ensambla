package com.ensambla.backend.infrastructure.presentation.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.GenerarCodigoWhatsappRequest;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoWhatsappResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class WhatsappPresentationMapperTest {

    private final WhatsappPresentationMapper whatsappPresentationMapper = new WhatsappPresentationMapper();

    @Test
    @DisplayName("Arma el pedido del código con el usuario de la sesión")
    void toGenerarCodigoRequestShouldPassTheUser() {
        GenerarCodigoWhatsappRequest result = whatsappPresentationMapper.toGenerarCodigoRequest(USUARIO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Devuelve el código y su vencimiento")
    void toResponseShouldMapCodigoAndVencimiento() {
        CodigoWhatsappResponse result = whatsappPresentationMapper.toResponse(
                new CodigoWhatsappItem(CODIGO, AHORA.plusMinutes(10)));

        assertThat(result.codigo()).isEqualTo(CODIGO);
        assertThat(result.venceEn()).isEqualTo(AHORA.plusMinutes(10));
    }
}
