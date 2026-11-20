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
import static org.assertj.core.api.Assertions.catchThrowable;

class PagosCajaTest {

    private VentaCaja venta;
    private Throwable rechazo;

    @Test
    @DisplayName("El pago mixto conserva las partes y calcula el vuelto sólo sobre el efectivo")
    void elPagoMixtoConservaLasPartesYCalculaElVueltoSoloSobreElEfectivo() {
        cuandoSeArmaLaVentaConPagoMixto();

        entoncesConservaLasTresPartesYElVueltoDelEfectivo();
    }

    @ParameterizedTest
    @CsvSource({"0,EFECTIVO", "-1,TRANSFERENCIA", "0.001,TARJETA", "1,MIXTO"})
    @DisplayName("Rechaza una parte sin importe válido o sin medio concreto")
    void rechazaUnaParteSinImporteValidoOSinMedioConcreto(String importe, MedioPagoCaja medio) {
        cuandoSeIntentaCrearUnaParte(importe, medio);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza una lista de pagos vacía")
    void rechazaUnaListaDePagosVacia() {
        cuandoSeIntentaVenderPagando(List.of(), null);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza más de tres pagos")
    void rechazaMasDeTresPagos() {
        cuandoSeIntentaVenderPagando(List.of(pago("500", MedioPagoCaja.EFECTIVO),
                pago("500", MedioPagoCaja.TARJETA), pago("500", MedioPagoCaja.TRANSFERENCIA),
                pago("500", MedioPagoCaja.EFECTIVO)), null);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza medios repetidos aunque cubran el total")
    void rechazaMediosRepetidosAunqueCubranElTotal() {
        cuandoSeIntentaVenderPagando(List.of(pago("1000", MedioPagoCaja.EFECTIVO),
                pago("1000", MedioPagoCaja.EFECTIVO)), null);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza pagos que no cubren exactamente la compra")
    void rechazaPagosQueNoCubrenExactamenteLaCompra() {
        cuandoSeIntentaVenderPagando(List.of(pago("1000", MedioPagoCaja.TARJETA)), null);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza efectivo recibido con fracciones de centavo")
    void rechazaEfectivoRecibidoConFraccionesDeCentavo() {
        cuandoSeIntentaVenderPagando(List.of(pago("2000", MedioPagoCaja.EFECTIVO)),
                new BigDecimal("2000.001"));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza un cobro mayor al total aunque haya cliente")
    void rechazaUnCobroMayorAlTotalAunqueHayaCliente() {
        cuandoSeIntentaCobrarDeMasAUnCliente();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    // --- cuando ---

    private void cuandoSeArmaLaVentaConPagoMixto() {
        venta = CierreCajaTestData.venta();
    }

    private void cuandoSeIntentaCrearUnaParte(String importe, MedioPagoCaja medio) {
        rechazo = catchThrowable(() -> pago(importe, medio));
    }

    private void cuandoSeIntentaVenderPagando(List<PagoCaja> pagos, BigDecimal recibido) {
        VentaCaja base = CierreCajaTestData.venta();
        rechazo = catchThrowable(() -> VentaCaja.nueva(base.id(), base.negocioId(), base.proyectoId(),
                base.inventarioId(), base.huella(), base.lineas(), MedioPagoCaja.EFECTIVO, recibido,
                base.creadoEn(), pagos));
    }

    private void cuandoSeIntentaCobrarDeMasAUnCliente() {
        VentaCaja base = CierreCajaTestData.venta();
        rechazo = catchThrowable(() -> VentaCaja.nueva(base.id(), base.negocioId(), base.proyectoId(),
                base.inventarioId(), base.huella(), base.lineas(), MedioPagoCaja.TRANSFERENCIA, null,
                base.creadoEn(), List.of(pago("2001", MedioPagoCaja.TRANSFERENCIA)), CajaTestData.CLIENTE,
                "Ana"));
    }

    // --- entonces ---

    private void entoncesConservaLasTresPartesYElVueltoDelEfectivo() {
        assertThat(venta.total()).isEqualByComparingTo("2000");
        assertThat(venta.medio()).isEqualTo(MedioPagoCaja.MIXTO);
        assertThat(venta.recibido()).isEqualByComparingTo("2500");
        assertThat(venta.vuelto()).isEqualByComparingTo("500");
        assertThat(venta.inventarioId()).isEqualTo(CajaTestData.INVENTARIO);
        assertThat(venta.pagos()).containsExactly(pago("1000", MedioPagoCaja.EFECTIVO),
                pago("700", MedioPagoCaja.TRANSFERENCIA), pago("300", MedioPagoCaja.TARJETA));
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    // --- datos ---

    private PagoCaja pago(String importe, MedioPagoCaja medio) {
        return new PagoCaja(medio, new BigDecimal(importe));
    }
}
