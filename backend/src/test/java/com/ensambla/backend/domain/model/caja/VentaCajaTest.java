package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class VentaCajaTest {

    private LineaVentaCaja linea;
    private VentaCaja venta;
    private CobrarVentaResponse respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Calcula el importe por peso con redondeo comercial por línea")
    void calculaElImportePorPesoConRedondeoComercialPorLinea() {
        cuandoSeVendePorPeso("0.125", "10999");

        entoncesElImporteSeRedondeaPorLinea();
    }

    @ParameterizedTest
    @CsvSource({"0,4000,PESO", "-1,4000,PESO", "0.0001,4000,PESO", "1,0,UNIDAD", "1,-1,UNIDAD",
            "1,4.001,UNIDAD", "1.5,4000,UNIDAD", "0.001,0.01,PESO"})
    @DisplayName("Rechaza cantidades y precios no representables sin inventar importes")
    void rechazaCantidadesYPreciosNoRepresentablesSinInventarImportes(String cantidad, String precio,
            ModoVenta modo) {
        cuandoSeIntentaVender(cantidad, precio, modo);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Acepta unidades enteras escritas con decimales de cero")
    void aceptaUnidadesEnterasEscritasConDecimalesDeCero() {
        cuandoSeVendenUnidades("2.000", "40.00");

        entoncesElImporteEs("80");
    }

    @Test
    @DisplayName("Conserva total, recibido, vuelto y detalle histórico en la respuesta")
    void conservaTotalRecibidoVueltoYDetalleHistoricoEnLaRespuesta() {
        cuandoSeCobraEnEfectivo(new BigDecimal("5000"));

        entoncesLaVentaConservaElComprobante();
        entoncesLaRespuestaConservaElDetalleHistorico();
    }

    @Test
    @DisplayName("Sin importe recibido registra el pago exacto")
    void sinImporteRecibidoRegistraElPagoExacto() {
        cuandoSeCobraEnEfectivo(null);

        entoncesNoHayVuelto();
    }

    @Test
    @DisplayName("Los pagos externos registran sólo el total y no generan vuelto")
    void losPagosExternosRegistranSoloElTotalYNoGeneranVuelto() {
        cuandoSeCobraConTarjeta(BigDecimal.ONE);

        entoncesSeRegistraSoloElTotal();
    }

    @Test
    @DisplayName("Rechaza un efectivo insuficiente")
    void rechazaUnEfectivoInsuficiente() {
        cuandoSeIntentaCobrarEnEfectivo(BigDecimal.ONE);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza la compra vacía")
    void rechazaLaCompraVacia() {
        cuandoSeIntentaCobrarUnaCompraVacia();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    // --- cuando ---

    private void cuandoSeVendePorPeso(String cantidad, String precio) {
        linea = unaLinea(cantidad, precio, ModoVenta.PESO);
    }

    private void cuandoSeVendenUnidades(String cantidad, String precio) {
        linea = unaLinea(cantidad, precio, ModoVenta.UNIDAD);
    }

    private void cuandoSeIntentaVender(String cantidad, String precio, ModoVenta modo) {
        rechazo = catchThrowable(() -> unaLinea(cantidad, precio, modo));
    }

    private void cuandoSeCobraEnEfectivo(BigDecimal recibido) {
        venta = unaVenta(MedioPagoCaja.EFECTIVO, recibido);
        respuesta = CobrarVentaResponse.de(venta);
    }

    private void cuandoSeCobraConTarjeta(BigDecimal recibido) {
        venta = unaVenta(MedioPagoCaja.TARJETA, recibido);
    }

    private void cuandoSeIntentaCobrarEnEfectivo(BigDecimal recibido) {
        rechazo = catchThrowable(() -> unaVenta(MedioPagoCaja.EFECTIVO, recibido));
    }

    private void cuandoSeIntentaCobrarUnaCompraVacia() {
        rechazo = catchThrowable(() -> VentaCaja.nueva(CajaTestData.OPERACION, CajaTestData.NEGOCIO,
                CajaTestData.PROYECTO, "huella", List.of(), MedioPagoCaja.EFECTIVO, null, CajaTestData.AHORA));
    }

    // --- entonces ---

    private void entoncesElImporteSeRedondeaPorLinea() {
        assertThat(linea.importe()).isEqualByComparingTo("1374.88");
        assertThat(linea.productoId()).isEqualTo(CajaTestData.PRODUCTO);
        assertThat(linea.nombre()).isEqualTo("Pan");
        assertThat(linea.modo()).isEqualTo(ModoVenta.PESO);
        assertThat(linea.cantidad()).isEqualByComparingTo("0.125");
        assertThat(linea.precio()).isEqualByComparingTo("10999");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesElImporteEs(String importe) {
        assertThat(linea.importe()).isEqualByComparingTo(importe);
    }

    private void entoncesLaVentaConservaElComprobante() {
        assertThat(venta.total()).isEqualByComparingTo("2000");
        assertThat(venta.recibido()).isEqualByComparingTo("5000");
        assertThat(venta.vuelto()).isEqualByComparingTo("3000");
        assertThat(venta.id()).isEqualTo(CajaTestData.OPERACION);
        assertThat(venta.negocioId()).isEqualTo(CajaTestData.NEGOCIO);
        assertThat(venta.proyectoId()).isEqualTo(CajaTestData.PROYECTO);
        assertThat(venta.huella()).isEqualTo("huella");
        assertThat(venta.creadoEn()).isEqualTo(CajaTestData.AHORA);
    }

    private void entoncesLaRespuestaConservaElDetalleHistorico() {
        assertThat(respuesta.id()).isEqualTo(venta.id());
        assertThat(respuesta.medio()).isEqualTo("EFECTIVO");
        assertThat(respuesta.total()).isEqualTo(venta.total());
        assertThat(respuesta.recibido()).isEqualTo(venta.recibido());
        assertThat(respuesta.vuelto()).isEqualTo(venta.vuelto());
        assertThat(respuesta.creadoEn()).isEqualTo(venta.creadoEn());
        var detalle = respuesta.lineas().getFirst();
        assertThat(detalle.productoId()).isEqualTo(CajaTestData.PRODUCTO);
        assertThat(detalle.nombre()).isEqualTo("Pan");
        assertThat(detalle.modo()).isEqualTo("PESO");
        assertThat(detalle.cantidad()).isEqualByComparingTo("0.5");
        assertThat(detalle.precio()).isEqualByComparingTo("4000");
        assertThat(detalle.importe()).isEqualByComparingTo("2000");
    }

    private void entoncesNoHayVuelto() {
        assertThat(venta.vuelto()).isEqualByComparingTo("0");
    }

    private void entoncesSeRegistraSoloElTotal() {
        assertThat(venta.recibido()).isEqualByComparingTo("2000");
    }

    // --- datos ---

    private LineaVentaCaja unaLinea(String cantidad, String precio, ModoVenta modo) {
        return LineaVentaCaja.de(CajaTestData.PRODUCTO, "Pan", modo,
                new BigDecimal(cantidad), new BigDecimal(precio));
    }

    private VentaCaja unaVenta(MedioPagoCaja medio, BigDecimal recibido) {
        return VentaCaja.nueva(CajaTestData.OPERACION, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                "huella", List.of(unaLinea("0.5", "4000", ModoVenta.PESO)), medio, recibido,
                CajaTestData.AHORA);
    }
}
