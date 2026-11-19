package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.support.CierreCajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CuentaClienteCajaItemTest {

    private ListarVentasCajaResponse respuesta;
    private CuentaClienteCajaItem cuenta;

    @Test
    @DisplayName("Agrupa deuda y cobros del cliente y omite ventas anónimas")
    void agrupaDeudaYCobrosDelClienteYOmiteVentasAnonimas() {
        cuandoSeAgrupanLasVentasDeUnClienteYUnaAnonima();

        entoncesSeAgrupaLaDeudaYLosCobrosDelCliente();
    }

    @Test
    @DisplayName("Una cuenta saldada conserva las ventas y limpia la fecha de deuda")
    void unaCuentaSaldadaConservaLasVentasYLimpiaLaFechaDeDeuda() {
        cuandoSeConsultaUnaCuentaSaldada();

        entoncesNoQuedaDeudaPeroSeConservaLaVenta();
    }

    @Test
    @DisplayName("La cuenta de un cliente no incluye los cobros de otra persona")
    void laCuentaDeUnClienteNoIncluyeLosCobrosDeOtraPersona() {
        cuandoSeConsultaLaCuentaConUnCobroDeOtroCliente();

        entoncesLaCuentaNoIncluyeElCobroAjeno();
    }

    // --- cuando ---

    private void cuandoSeAgrupanLasVentasDeUnClienteYUnaAnonima() {
        respuesta = ListarVentasCajaResponse.de(List.of(CierreCajaTestData.venta(), CobroCajaTestData.venta()),
                List.of(CobroCajaTestData.cobro()));
    }

    private void cuandoSeConsultaUnaCuentaSaldada() {
        VentaCaja pagada = CobroCajaTestData.venta().conCobro(
                List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("1200"))));
        cuenta = CuentaClienteCajaItem.de(List.of(pagada), List.of());
    }

    private void cuandoSeConsultaLaCuentaConUnCobroDeOtroCliente() {
        CobroCaja base = CobroCajaTestData.cobro();
        CobroCaja ajeno = new CobroCaja(base.id(), base.negocioId(), base.proyectoId(), base.inventarioId(),
                RegistroId.nuevo(), "Otro cliente", base.huella(), base.aplicaciones(), base.creadoEn());
        cuenta = CuentaClienteCajaItem.de(List.of(CobroCajaTestData.venta()), List.of(ajeno));
    }

    // --- entonces ---

    private void entoncesSeAgrupaLaDeudaYLosCobrosDelCliente() {
        assertThat(respuesta.cuentas()).hasSize(1);
        CuentaClienteCajaItem delCliente = respuesta.cuentas().getFirst();
        assertThat(delCliente.clienteId()).isEqualTo(CobroCajaTestData.venta().clienteId());
        assertThat(delCliente.clienteNombre()).isEqualTo("Ana López");
        assertThat(delCliente.saldo()).isEqualByComparingTo("1200");
        assertThat(delCliente.cantidadPendientes()).isEqualTo(1);
        assertThat(delCliente.pendienteDesde()).isEqualTo(CobroCajaTestData.venta().creadoEn());
        assertThat(delCliente.ventas()).hasSize(1);
        assertThat(delCliente.cobros()).hasSize(1);
    }

    private void entoncesNoQuedaDeudaPeroSeConservaLaVenta() {
        assertThat(cuenta.saldo()).isEqualByComparingTo("0");
        assertThat(cuenta.cantidadPendientes()).isZero();
        assertThat(cuenta.pendienteDesde()).isNull();
        assertThat(cuenta.ventas()).hasSize(1);
    }

    private void entoncesLaCuentaNoIncluyeElCobroAjeno() {
        assertThat(cuenta.cobros()).isEmpty();
        assertThat(cuenta.saldo()).isEqualByComparingTo("1200");
    }
}
