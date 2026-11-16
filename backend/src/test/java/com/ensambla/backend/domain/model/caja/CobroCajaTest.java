package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobroCajaTest {

    @Test
    @DisplayName("Un cobro conserva fecha, cliente, medios y aplicaciones en su respuesta")
    void deShouldPreserveCollectionDetails() {
        CobroCaja cobro = CobroCajaTestData.cobro();

        CobroCajaItem respuesta = CobroCajaItem.de(cobro);

        assertThat(respuesta.id()).isEqualTo(cobro.id());
        assertThat(respuesta.clienteId()).isEqualTo(CajaTestData.CLIENTE);
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
        assertThat(respuesta.creadoEn()).isEqualTo(cobro.creadoEn());
        assertThat(respuesta.importe()).isEqualByComparingTo("600");
        assertThat(respuesta.aplicaciones().getFirst().ventaId()).isEqualTo(CajaTestData.OPERACION);
        assertThat(respuesta.aplicaciones().getFirst().importe()).isEqualByComparingTo("600");
        assertThat(respuesta.aplicaciones().getFirst().pagos().getFirst().medio()).isEqualTo("TRANSFERENCIA");
        assertThat(respuesta.aplicaciones().getFirst().pagos().getFirst().importe()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("Un pago posterior reduce el saldo sin alterar el importe cobrado al vender")
    void conCobroShouldPreserveOriginalPayments() {
        VentaCaja original = CobroCajaTestData.venta();

        VentaCaja actual = original.conCobro(List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("600"))));

        assertThat(actual.saldo()).isEqualByComparingTo("600");
        assertThat(actual.cobrado()).isEqualByComparingTo("1400");
        assertThat(actual.pagosIniciales()).isEqualTo(original.pagos());
        assertThat(actual.medio()).isEqualTo(MedioPagoCaja.MIXTO);
        assertThat(original.saldo()).isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("Completar una deuda cambia automáticamente el estado a pagada")
    void conCobroShouldSettleCreditSale() {
        VentaCaja original = CobroCajaTestData.venta(CajaTestData.OPERACION, "0");

        VentaCaja actual = original.conCobro(List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("2000"))));

        assertThat(actual.estado()).isEqualTo(EstadoVentaCaja.PAGADA);
        assertThat(actual.saldo()).isEqualByComparingTo("0");
        assertThat(actual.medio()).isEqualTo(MedioPagoCaja.TRANSFERENCIA);
    }

    @Test
    @DisplayName("Acumula entregas del mismo medio sin duplicar el cobro inicial")
    void conCobroShouldAccumulateSameMethod() {
        VentaCaja original = CobroCajaTestData.venta();

        VentaCaja actual = original.conCobro(List.of(new PagoCaja(MedioPagoCaja.EFECTIVO, new BigDecimal("600"))));

        assertThat(actual.pagos()).containsExactly(new PagoCaja(MedioPagoCaja.EFECTIVO, new BigDecimal("1400")));
    }

    @Test
    @DisplayName("Una entrega superior al saldo se rechaza")
    void conCobroShouldRejectOverpayment() {
        VentaCaja original = CobroCajaTestData.venta();

        assertThatThrownBy(() -> original.conCobro(List.of(new PagoCaja(MedioPagoCaja.EFECTIVO, new BigDecimal("1201")))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza un cobro sin ventas aplicadas")
    void constructorShouldRejectEmptyCollection() {
        assertThatThrownBy(() -> cobro(List.of())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza aplicar dos veces el mismo cobro a una venta")
    void constructorShouldRejectRepeatedSale() {
        CobroCaja.Aplicacion aplicacion = CobroCajaTestData.cobro().aplicaciones().getFirst();

        assertThatThrownBy(() -> cobro(List.of(aplicacion, aplicacion))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una aplicación sin pagos")
    void applicationShouldRejectEmptyPayments() {
        assertThatThrownBy(() -> new CobroCaja.Aplicacion(CajaTestData.OPERACION, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza medios repetidos en la misma aplicación")
    void applicationShouldRejectRepeatedMethods() {
        PagoCaja pago = new PagoCaja(MedioPagoCaja.EFECTIVO, BigDecimal.ONE);

        assertThatThrownBy(() -> new CobroCaja.Aplicacion(CajaTestData.OPERACION, List.of(pago, pago)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza más de tres partes en una aplicación")
    void applicationShouldRejectTooManyMethods() {
        PagoCaja pago = new PagoCaja(MedioPagoCaja.EFECTIVO, BigDecimal.ONE);

        assertThatThrownBy(() -> new CobroCaja.Aplicacion(CajaTestData.OPERACION, List.of(pago, pago, pago, pago)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("El resumen del día suma sólo el cobro inicial y los movimientos recibidos ese día")
    void resumenShouldSeparateInitialAndLaterMoney() {
        VentaCaja venta = CobroCajaTestData.venta().conCobro(CobroCajaTestData.cobro().aplicaciones().getFirst().pagos());

        ResumenCaja resumen = ResumenCaja.de(List.of(venta), List.of(CobroCajaTestData.cobro()));

        assertThat(resumen.total()).isEqualByComparingTo("2000");
        assertThat(resumen.efectivo()).isEqualByComparingTo("800");
        assertThat(resumen.transferencia()).isEqualByComparingTo("600");
        assertThat(resumen.tarjeta()).isEqualByComparingTo("0");
        assertThat(resumen.desde()).isEqualTo(CajaTestData.AHORA);
    }

    // --- helpers ---
    private CobroCaja cobro(List<CobroCaja.Aplicacion> aplicaciones) {
        CobroCaja base = CobroCajaTestData.cobro();
        return new CobroCaja(base.id(), base.negocioId(), base.proyectoId(), base.inventarioId(),
                base.clienteId(), base.clienteNombre(), base.huella(), aplicaciones, base.creadoEn());
    }
}
