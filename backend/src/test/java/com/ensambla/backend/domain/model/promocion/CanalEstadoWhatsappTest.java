package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PromocionDePrueba.promocionDeYerba;
import static org.assertj.core.api.Assertions.assertThat;

class CanalEstadoWhatsappTest extends CanalPromocionContratoTest {

    @Override
    protected CanalPromocion crear() {
        return new CanalEstadoWhatsapp();
    }

    @Test
    @DisplayName("Se llama estado")
    void claveShouldBeEstado() {
        assertThat(crear().clave()).isEqualTo("estado");
        assertThat(crear().nombre()).isEqualTo("Estado de WhatsApp");
    }

    @Test
    @DisplayName("Escribe el título en negrita de WhatsApp, la lista y el nombre del negocio al final")
    void redactarShouldWriteAShortStatus() {
        String result = crear().redactar(NEGOCIO, promocionDeYerba());

        assertThat(result).isEqualTo("*Tu producto estrella*\nLo que más se vendió este mes\n\n"
                + "• Yerba Playadito 1 kg: $ 3.600\n\nAlmacén de Ana");
    }
}
