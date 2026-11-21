package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VentaCajaImportePorMedioTest {

    private VentaCaja venta;
    private BigDecimal importe;

    @Test
    @DisplayName("Suma lo cobrado por el medio pedido")
    void sumaLoCobradoPorElMedioPedido() {
        dadaUnaVentaCobradaEnEfectivoYPorTransferencia();

        cuandoSePideElImporteEn(MedioPagoCaja.TRANSFERENCIA);

        entoncesElImporteEs("600");
    }

    @Test
    @DisplayName("No mezcla lo cobrado por otros medios")
    void noMezclaLoCobradoPorOtrosMedios() {
        dadaUnaVentaCobradaEnEfectivoYPorTransferencia();

        cuandoSePideElImporteEn(MedioPagoCaja.EFECTIVO);

        entoncesElImporteEs("800");
    }

    @Test
    @DisplayName("Un medio sin pagos suma cero")
    void unMedioSinPagosSumaCero() {
        dadaUnaVentaCobradaEnEfectivoYPorTransferencia();

        cuandoSePideElImporteEn(MedioPagoCaja.TARJETA);

        entoncesElImporteEs("0");
    }

    // --- dado ---
    private void dadaUnaVentaCobradaEnEfectivoYPorTransferencia() {
        venta = CobroCajaTestData.venta().conCobro(List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA,
                new BigDecimal("600"))));
    }

    // --- cuando ---
    private void cuandoSePideElImporteEn(MedioPagoCaja medio) {
        importe = venta.importeEn(medio);
    }

    // --- entonces ---
    private void entoncesElImporteEs(String esperado) {
        assertThat(importe).isEqualByComparingTo(esperado);
        assertThat(venta.negocioId()).isEqualTo(CajaTestData.NEGOCIO);
    }
}
