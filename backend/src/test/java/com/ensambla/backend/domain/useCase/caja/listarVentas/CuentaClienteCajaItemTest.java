package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.support.CobroCajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CuentaClienteCajaItemTest {
    @Test
    @DisplayName("Agrupa deuda y cobros del cliente y omite ventas anónimas")
    void deShouldGroupCustomerDebtAndCollections() {
        ListarVentasCajaResponse respuesta = whenSeAgrupanVentas();

        thenAgrupaDeudaDelCliente(respuesta);
    }

    @Test
    @DisplayName("Una cuenta saldada conserva las ventas y limpia la fecha de deuda")
    void deShouldKeepPaidHistoryWithoutPendingDate() {
        CuentaClienteCajaItem cuenta = whenSeConsultaCuentaSaldada();

        thenNoQuedaDeuda(cuenta);
    }

    @Test
    @DisplayName("La cuenta de un cliente no incluye los cobros de otra persona")
    void deShouldExcludeOtherCustomerCollections() {
        CuentaClienteCajaItem cuenta = whenSeConsultaConCobroAjeno();

        thenNoIncluyeCobroAjeno(cuenta);
    }

    private CuentaClienteCajaItem whenSeConsultaConCobroAjeno() {
        CobroCaja base = CobroCajaTestData.cobro();
        CobroCaja ajeno = new CobroCaja(base.id(), base.negocioId(), base.proyectoId(), base.inventarioId(),
                RegistroId.nuevo(), "Otro cliente", base.huella(), base.aplicaciones(), base.creadoEn());
        return CuentaClienteCajaItem.de(List.of(CobroCajaTestData.venta()), List.of(ajeno));
    }

    private void thenNoIncluyeCobroAjeno(CuentaClienteCajaItem cuenta) {
        assertThat(cuenta.cobros()).isEmpty();
        assertThat(cuenta.saldo()).isEqualByComparingTo("1200");
    }

    private ListarVentasCajaResponse whenSeAgrupanVentas() {
        return ListarVentasCajaResponse.de(List.of(CierreCajaTestData.venta(), CobroCajaTestData.venta()),
                List.of(CobroCajaTestData.cobro()));
    }

    private CuentaClienteCajaItem whenSeConsultaCuentaSaldada() {
        VentaCaja pagada = CobroCajaTestData.venta().conCobro(
                List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("1200"))));
        return CuentaClienteCajaItem.de(List.of(pagada), List.of());
    }

    private void thenAgrupaDeudaDelCliente(ListarVentasCajaResponse respuesta) {
        assertThat(respuesta.cuentas()).hasSize(1);
        CuentaClienteCajaItem cuenta = respuesta.cuentas().getFirst();
        assertThat(cuenta.clienteId()).isEqualTo(CobroCajaTestData.venta().clienteId());
        assertThat(cuenta.clienteNombre()).isEqualTo("Ana López");
        assertThat(cuenta.saldo()).isEqualByComparingTo("1200");
        assertThat(cuenta.cantidadPendientes()).isEqualTo(1);
        assertThat(cuenta.pendienteDesde()).isEqualTo(CobroCajaTestData.venta().creadoEn());
        assertThat(cuenta.ventas()).hasSize(1);
        assertThat(cuenta.cobros()).hasSize(1);
    }

    private void thenNoQuedaDeuda(CuentaClienteCajaItem cuenta) {
        assertThat(cuenta.saldo()).isEqualByComparingTo("0");
        assertThat(cuenta.cantidadPendientes()).isZero();
        assertThat(cuenta.pendienteDesde()).isNull();
        assertThat(cuenta.ventas()).hasSize(1);
    }
}
