package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class EstadoCobroQrTest {

    private EstadoCobroQr visto;
    private Throwable rechazo;

    @Test
    @DisplayName("Un cobro pendiente que llegó a su vencimiento se ve vencido")
    void unCobroPendienteQueLlegoASuVencimientoSeVeVencido() {
        cuandoSeMiraElEstadoSegunElVencimiento(EstadoCobroQr.PENDIENTE, true);

        entoncesSeVe(EstadoCobroQr.VENCIDO);
    }

    @Test
    @DisplayName("Un cobro pendiente dentro de su vigencia sigue pendiente")
    void unCobroPendienteDentroDeSuVigenciaSigueSiendoPendiente() {
        cuandoSeMiraElEstadoSegunElVencimiento(EstadoCobroQr.PENDIENTE, false);

        entoncesSeVe(EstadoCobroQr.PENDIENTE);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCobroQr.class, names = {"VENCIDO", "COBRADO", "CANCELADO"})
    @DisplayName("Los demás estados no cambian aunque haya pasado el vencimiento")
    void losDemasEstadosNoCambianAunqueHayaPasadoElVencimiento(EstadoCobroQr estado) {
        cuandoSeMiraElEstadoSegunElVencimiento(estado, true);

        entoncesSeVe(estado);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCobroQr.class, names = {"PENDIENTE", "VENCIDO"})
    @DisplayName("Un cobro pendiente o vencido admite la confirmación")
    void unCobroPendienteOVencidoAdmiteLaConfirmacion(EstadoCobroQr estado) {
        cuandoSeIntentaConfirmar(estado);

        entoncesNoSeRechaza();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCobroQr.class, names = {"PENDIENTE", "VENCIDO"})
    @DisplayName("Un cobro pendiente o vencido admite la cancelación")
    void unCobroPendienteOVencidoAdmiteLaCancelacion(EstadoCobroQr estado) {
        cuandoSeIntentaCancelar(estado);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Un cobro ya cobrado no se confirma de nuevo")
    void unCobroYaCobradoNoSeConfirmaDeNuevo() {
        cuandoSeIntentaConfirmar(EstadoCobroQr.COBRADO);

        entoncesSeRechazaPorque("El cobro con QR ya está cobrado");
    }

    @Test
    @DisplayName("Un cobro cancelado no se puede confirmar")
    void unCobroCanceladoNoSePuedeConfirmar() {
        cuandoSeIntentaConfirmar(EstadoCobroQr.CANCELADO);

        entoncesSeRechazaPorque("El cobro con QR está cancelado");
    }

    @Test
    @DisplayName("Un cobro ya cobrado no se puede cancelar")
    void unCobroYaCobradoNoSePuedeCancelar() {
        cuandoSeIntentaCancelar(EstadoCobroQr.COBRADO);

        entoncesSeRechazaPorque("El cobro con QR ya está cobrado");
    }

    @Test
    @DisplayName("Un cobro cancelado no se cancela de nuevo")
    void unCobroCanceladoNoSeCancelaDeNuevo() {
        cuandoSeIntentaCancelar(EstadoCobroQr.CANCELADO);

        entoncesSeRechazaPorque("El cobro con QR está cancelado");
    }

    // --- cuando ---
    private void cuandoSeMiraElEstadoSegunElVencimiento(EstadoCobroQr estado, boolean vencio) {
        visto = estado.segunVencimiento(vencio);
    }

    private void cuandoSeIntentaConfirmar(EstadoCobroQr estado) {
        rechazo = catchThrowable(estado::exigirQueAdmitaConfirmacion);
    }

    private void cuandoSeIntentaCancelar(EstadoCobroQr estado) {
        rechazo = catchThrowable(estado::exigirQueAdmitaCancelacion);
    }

    // --- entonces ---
    private void entoncesSeVe(EstadoCobroQr esperado) {
        assertThat(visto).isEqualTo(esperado);
    }

    private void entoncesNoSeRechaza() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }
}
