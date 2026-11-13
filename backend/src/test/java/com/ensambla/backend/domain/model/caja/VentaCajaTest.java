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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VentaCajaTest {

    @Test
    @DisplayName("Calcula el importe por peso con redondeo comercial por línea")
    void lineShouldRoundWhenSoldByWeight() {
        LineaVentaCaja linea = linea("0.125", "10999", ModoVenta.PESO);

        assertThat(linea.importe()).isEqualByComparingTo("1374.88");
        assertThat(linea.productoId()).isEqualTo(CajaTestData.PRODUCTO);
        assertThat(linea.nombre()).isEqualTo("Pan");
        assertThat(linea.modo()).isEqualTo(ModoVenta.PESO);
        assertThat(linea.cantidad()).isEqualByComparingTo("0.125");
        assertThat(linea.precio()).isEqualByComparingTo("10999");
    }

    @ParameterizedTest
    @CsvSource({"0,4000,PESO", "-1,4000,PESO", "0.0001,4000,PESO", "1,0,UNIDAD", "1,-1,UNIDAD",
            "1,4.001,UNIDAD", "1.5,4000,UNIDAD", "0.001,0.01,PESO"})
    @DisplayName("Rechaza cantidades y precios no representables sin inventar importes")
    void lineShouldRejectInvalidAmounts(String cantidad, String precio, ModoVenta modo) {
        assertThatThrownBy(() -> linea(cantidad, precio, modo)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Acepta unidades enteras escritas con decimales de cero")
    void lineShouldAcceptWholeUnits() {
        LineaVentaCaja linea = linea("2.000", "40.00", ModoVenta.UNIDAD);

        assertThat(linea.importe()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("Conserva total, recibido, vuelto y detalle histórico en la respuesta")
    void saleShouldPreserveReceipt() {
        VentaCaja venta = venta(MedioPagoCaja.EFECTIVO, new BigDecimal("5000"));

        CobrarVentaResponse respuesta = CobrarVentaResponse.de(venta);

        assertThat(venta.total()).isEqualByComparingTo("2000");
        assertThat(venta.recibido()).isEqualByComparingTo("5000");
        assertThat(venta.vuelto()).isEqualByComparingTo("3000");
        assertThat(venta.id()).isEqualTo(CajaTestData.OPERACION);
        assertThat(venta.negocioId()).isEqualTo(CajaTestData.NEGOCIO);
        assertThat(venta.proyectoId()).isEqualTo(CajaTestData.PROYECTO);
        assertThat(venta.huella()).isEqualTo("huella");
        assertThat(venta.creadoEn()).isEqualTo(CajaTestData.AHORA);
        assertThat(respuesta.id()).isEqualTo(venta.id());
        assertThat(respuesta.medio()).isEqualTo("EFECTIVO");
        assertThat(respuesta.total()).isEqualTo(venta.total());
        assertThat(respuesta.recibido()).isEqualTo(venta.recibido());
        assertThat(respuesta.vuelto()).isEqualTo(venta.vuelto());
        assertThat(respuesta.creadoEn()).isEqualTo(venta.creadoEn());
        var linea = respuesta.lineas().getFirst();
        assertThat(linea.productoId()).isEqualTo(CajaTestData.PRODUCTO);
        assertThat(linea.nombre()).isEqualTo("Pan");
        assertThat(linea.modo()).isEqualTo("PESO");
        assertThat(linea.cantidad()).isEqualByComparingTo("0.5");
        assertThat(linea.precio()).isEqualByComparingTo("4000");
        assertThat(linea.importe()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("Sin importe recibido registra el pago exacto")
    void saleShouldUseExactAmountWhenReceivedIsMissing() {
        VentaCaja venta = venta(MedioPagoCaja.EFECTIVO, null);

        assertThat(venta.vuelto()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Los pagos externos registran sólo el total y no generan vuelto")
    void saleShouldUseTotalWhenPaidByCard() {
        VentaCaja venta = venta(MedioPagoCaja.TARJETA, BigDecimal.ONE);

        assertThat(venta.recibido()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("Rechaza un efectivo insuficiente")
    void saleShouldRejectInsufficientCash() {
        assertThatThrownBy(() -> venta(MedioPagoCaja.EFECTIVO, BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza la compra vacía")
    void saleShouldRejectEmptyCart() {
        assertThatThrownBy(() -> VentaCaja.nueva(CajaTestData.OPERACION, CajaTestData.NEGOCIO,
                CajaTestData.PROYECTO, "huella", List.of(), MedioPagoCaja.EFECTIVO, null, CajaTestData.AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- helpers ---
    private LineaVentaCaja linea(String cantidad, String precio, ModoVenta modo) {
        return LineaVentaCaja.de(CajaTestData.PRODUCTO, "Pan", modo,
                new BigDecimal(cantidad), new BigDecimal(precio));
    }

    private VentaCaja venta(MedioPagoCaja medio, BigDecimal recibido) {
        return VentaCaja.nueva(CajaTestData.OPERACION, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                "huella", List.of(linea("0.5", "4000", ModoVenta.PESO)), medio, recibido, CajaTestData.AHORA);
    }
}
