package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagosCajaTest {

    @Test
    @DisplayName("El pago mixto conserva las partes y calcula el vuelto sólo sobre el efectivo")
    void nuevaShouldSplitPaymentsAndChange() {
        VentaCaja venta = CierreCajaTestData.venta();

        assertThat(venta.total()).isEqualByComparingTo("2000");
        assertThat(venta.medio()).isEqualTo(MedioPagoCaja.MIXTO);
        assertThat(venta.recibido()).isEqualByComparingTo("2500");
        assertThat(venta.vuelto()).isEqualByComparingTo("500");
        assertThat(venta.inventarioId()).isEqualTo(CajaTestData.INVENTARIO);
        assertThat(venta.pagos()).containsExactly(pago("1000", MedioPagoCaja.EFECTIVO),
                pago("700", MedioPagoCaja.TRANSFERENCIA), pago("300", MedioPagoCaja.TARJETA));
    }

    @ParameterizedTest
    @CsvSource({"0,EFECTIVO", "-1,TRANSFERENCIA", "0.001,TARJETA", "1,MIXTO"})
    @DisplayName("Rechaza una parte sin importe válido o sin medio concreto")
    void pagoShouldRejectInvalidPart(String importe, MedioPagoCaja medio) {
        assertThatThrownBy(() -> pago(importe, medio)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una lista de pagos vacía")
    void nuevaShouldRejectEmptyPayments() {
        thenRejects(List.of(), null);
    }

    @Test
    @DisplayName("Rechaza más de tres pagos")
    void nuevaShouldRejectTooManyPayments() {
        thenRejects(List.of(pago("500", MedioPagoCaja.EFECTIVO), pago("500", MedioPagoCaja.TARJETA),
                pago("500", MedioPagoCaja.TRANSFERENCIA), pago("500", MedioPagoCaja.EFECTIVO)), null);
    }

    @Test
    @DisplayName("Rechaza medios repetidos aunque cubran el total")
    void nuevaShouldRejectRepeatedPaymentMethod() {
        thenRejects(List.of(pago("1000", MedioPagoCaja.EFECTIVO), pago("1000", MedioPagoCaja.EFECTIVO)), null);
    }

    @Test
    @DisplayName("Rechaza pagos que no cubren exactamente la compra")
    void nuevaShouldRejectUnbalancedPayments() {
        thenRejects(List.of(pago("1000", MedioPagoCaja.TARJETA)), null);
    }

    @Test
    @DisplayName("Rechaza efectivo recibido con fracciones de centavo")
    void nuevaShouldRejectFractionalCash() {
        thenRejects(List.of(pago("2000", MedioPagoCaja.EFECTIVO)), new BigDecimal("2000.001"));
    }

    // --- assert ---
    private void thenRejects(List<PagoCaja> pagos, BigDecimal recibido) {
        VentaCaja base = CierreCajaTestData.venta();
        assertThatThrownBy(() -> VentaCaja.nueva(base.id(), base.negocioId(), base.proyectoId(),
                base.inventarioId(), base.huella(), base.lineas(), MedioPagoCaja.EFECTIVO, recibido,
                base.creadoEn(), pagos)).isInstanceOf(BusinessRuleException.class);
    }

    // --- helpers ---
    private PagoCaja pago(String importe, MedioPagoCaja medio) {
        return new PagoCaja(medio, new BigDecimal(importe));
    }
}
