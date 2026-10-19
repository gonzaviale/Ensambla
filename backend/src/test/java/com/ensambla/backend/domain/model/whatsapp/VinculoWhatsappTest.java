package com.ensambla.backend.domain.model.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.vinculado;
import static com.ensambla.backend.support.WhatsappTestData.vinculoConCodigo;
import static org.assertj.core.api.Assertions.assertThat;

class VinculoWhatsappTest {

    @Test
    @DisplayName("Un vínculo nuevo es del negocio, sin número ni código")
    void nuevoShouldStartWithoutTelefonoNorCodigo() {
        VinculoWhatsapp result = VinculoWhatsapp.nuevo(NEGOCIO_ID, AHORA);

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTelefono()).isNull();
        assertThat(result.getCodigo()).isNull();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("El código vence a los 10 minutos")
    void asignarCodigoShouldExpireAfterTenMinutes() {
        VinculoWhatsapp vinculo = VinculoWhatsapp.nuevo(NEGOCIO_ID, AHORA);

        vinculo.asignarCodigo(CODIGO, AHORA);

        assertThat(vinculo.getCodigo()).isEqualTo(CODIGO);
        assertThat(vinculo.getCodigoVenceEn()).isEqualTo(AHORA.plusMinutes(10));
    }

    @Test
    @DisplayName("Un código nuevo no cambia el número ya vinculado")
    void asignarCodigoShouldKeepTheLinkedTelefono() {
        VinculoWhatsapp vinculo = vinculado(NEGOCIO_ID, TELEFONO);

        vinculo.asignarCodigo(CODIGO, AHORA.plusDays(1));

        assertThat(vinculo.getTelefono()).isEqualTo(TELEFONO);
        assertThat(vinculo.getActualizadoEn()).isEqualTo(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("El código sirve antes de vencer")
    void codigoVigenteShouldBeTrueBeforeExpiry() {
        VinculoWhatsapp vinculo = vinculoConCodigo(NEGOCIO_ID);

        boolean result = vinculo.codigoVigente(AHORA.plusMinutes(9));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("El código no sirve en el instante en que vence ni después")
    void codigoVigenteShouldBeFalseOnceExpired() {
        VinculoWhatsapp vinculo = vinculoConCodigo(NEGOCIO_ID);

        assertThat(vinculo.codigoVigente(AHORA.plusMinutes(10))).isFalse();
        assertThat(vinculo.codigoVigente(AHORA.plusHours(1))).isFalse();
    }

    @Test
    @DisplayName("Sin código no hay nada vigente")
    void codigoVigenteShouldBeFalseWithoutCodigo() {
        VinculoWhatsapp vinculo = VinculoWhatsapp.nuevo(NEGOCIO_ID, AHORA);

        boolean result = vinculo.codigoVigente(AHORA);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Al vincular guarda el número y borra el código")
    void vincularShouldSaveTelefonoAndClearCodigo() {
        VinculoWhatsapp vinculo = vinculoConCodigo(NEGOCIO_ID);

        vinculo.vincular(TELEFONO, AHORA.plusMinutes(1));

        assertThat(vinculo.getTelefono()).isEqualTo(TELEFONO);
        assertThat(vinculo.getVinculadoEn()).isEqualTo(AHORA.plusMinutes(1));
        assertThat(vinculo.getCodigo()).isNull();
        assertThat(vinculo.getCodigoVenceEn()).isNull();
        assertThat(vinculo.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(1));
    }

    @Test
    @DisplayName("Al desvincular el negocio se queda sin número")
    void desvincularShouldClearTelefono() {
        VinculoWhatsapp vinculo = vinculado(NEGOCIO_ID, TELEFONO);

        vinculo.desvincular(AHORA.plusDays(1));

        assertThat(vinculo.getTelefono()).isNull();
        assertThat(vinculo.getVinculadoEn()).isNull();
        assertThat(vinculo.getActualizadoEn()).isEqualTo(AHORA.plusDays(1));
    }
}
