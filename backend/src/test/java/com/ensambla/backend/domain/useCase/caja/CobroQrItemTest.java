package com.ensambla.backend.domain.useCase.caja;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.elMomentoDelVencimiento;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCobrado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static org.assertj.core.api.Assertions.assertThat;

class CobroQrItemTest {

    private CobroQrItem item;

    @Test
    @DisplayName("Copia el importe, el alias, el titular y el vencimiento del cobro")
    void copiaElImporteElAliasElTitularYElVencimientoDelCobro() {
        cuandoSeArmaElItemDeUnCobroPendienteEn(AHORA);

        entoncesTieneLosDatosDelCobro();
    }

    @Test
    @DisplayName("Un cobro pendiente se muestra pendiente dentro de su vigencia")
    void unCobroPendienteSeMuestraPendienteDentroDeSuVigencia() {
        cuandoSeArmaElItemDeUnCobroPendienteEn(AHORA.plusMinutes(1));

        entoncesElEstadoEs("PENDIENTE");
    }

    @Test
    @DisplayName("Un cobro pendiente se muestra vencido pasada su vigencia")
    void unCobroPendienteSeMuestraVencidoPasadaSuVigencia() {
        cuandoSeArmaElItemDeUnCobroPendienteEn(elMomentoDelVencimiento());

        entoncesElEstadoEs("VENCIDO");
    }

    @Test
    @DisplayName("Un cobro cobrado se muestra cobrado")
    void unCobroCobradoSeMuestraCobrado() {
        cuandoSeArmaElItemDeUnCobroCobrado();

        entoncesElEstadoEs("COBRADO");
    }

    // --- cuando ---
    private void cuandoSeArmaElItemDeUnCobroPendienteEn(OffsetDateTime momento) {
        item = CobroQrItem.de(unCobroQrPendiente(), momento);
    }

    private void cuandoSeArmaElItemDeUnCobroCobrado() {
        item = CobroQrItem.de(unCobroQrCobrado(), AHORA.plusMinutes(6));
    }

    // --- entonces ---
    private void entoncesTieneLosDatosDelCobro() {
        assertThat(item.id()).isEqualTo(COBRO_QR_ID);
        assertThat(item.importe()).isEqualByComparingTo(IMPORTE);
        assertThat(item.alias()).isEqualTo("mi.alias.mp");
        assertThat(item.titular()).isEqualTo("Ana Pérez");
        assertThat(item.venceEn()).isEqualTo(elMomentoDelVencimiento());
    }

    private void entoncesElEstadoEs(String estado) {
        assertThat(item.estado()).isEqualTo(estado);
    }
}
