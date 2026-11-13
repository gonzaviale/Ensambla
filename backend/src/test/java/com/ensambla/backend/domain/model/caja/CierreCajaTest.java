package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
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

class CierreCajaTest {

    @Test
    @DisplayName("Guarda el resumen histórico y el arqueo sin sumar transferencias ni tarjetas al cajón")
    void nuevoShouldPreserveSnapshotAndCashCount() {
        CierreCaja cierre = CierreCajaTestData.cierre();

        assertThat(cierre.id()).isEqualTo(CierreCajaTestData.CIERRE);
        assertThat(cierre.negocioId()).isEqualTo(CajaTestData.NEGOCIO);
        assertThat(cierre.proyectoId()).isEqualTo(CajaTestData.PROYECTO);
        assertThat(cierre.inventarioId()).isEqualTo(CajaTestData.INVENTARIO);
        assertThat(cierre.huella()).isEqualTo(CierreCajaTestData.request().huella());
        assertThat(cierre.resumen().total()).isEqualByComparingTo("2000");
        assertThat(cierre.resumen().efectivo()).isEqualByComparingTo("1000");
        assertThat(cierre.resumen().transferencia()).isEqualByComparingTo("700");
        assertThat(cierre.resumen().tarjeta()).isEqualByComparingTo("300");
        assertThat(cierre.resumen().desde()).isEqualTo(CajaTestData.AHORA);
        assertThat(cierre.fondoInicial()).isEqualByComparingTo("500");
        assertThat(cierre.entradas()).isEqualByComparingTo("100");
        assertThat(cierre.retiros()).isEqualByComparingTo("200");
        assertThat(cierre.esperado()).isEqualByComparingTo("1400");
        assertThat(cierre.contado()).isEqualByComparingTo("1400");
        assertThat(cierre.diferencia()).isEqualByComparingTo("0");
        assertThat(cierre.nota()).isEqualTo("Revisado");
        assertThat(cierre.cerradoEn()).isEqualTo(CajaTestData.AHORA);
    }

    @Test
    @DisplayName("Permite guardar el cierre sin hacer un recuento")
    void nuevoShouldAllowMissingCount() {
        CierreCaja cierre = cerrar(resumen(), resumen().revision(), "0", "0", "0", null);

        assertThat(cierre.contado()).isNull();
        assertThat(cierre.diferencia()).isNull();
    }

    @Test
    @DisplayName("Un período vacío no genera un cierre")
    void nuevoShouldRejectEmptyPeriod() {
        assertThatThrownBy(() -> cerrar(ResumenCaja.de(List.of()), "", "0", "0", "0", null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Una venta nueva obliga a revisar el resumen antes de cerrar")
    void nuevoShouldRejectChangedRevision() {
        assertThatThrownBy(() -> cerrar(resumen(), "anterior", "0", "0", "0", null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @ParameterizedTest
    @CsvSource({"-1,0,0,0", "0.001,0,0,0", "0,-1,0,0", "0,0,-1,0", "0,0,0,-1",
            "0,0,1001,0", "0,0,0,0.001"})
    @DisplayName("Rechaza movimientos o recuentos inválidos")
    void nuevoShouldRejectInvalidCash(String fondo, String entradas, String retiros, String contado) {
        assertThatThrownBy(() -> cerrar(resumen(), resumen().revision(), fondo, entradas, retiros, contado))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("El resumen vacío conserva importes cero y no inventa fecha de apertura")
    void deShouldRepresentEmptyPeriod() {
        ResumenCaja resumen = ResumenCaja.de(List.of());

        assertThat(resumen.total()).isEqualByComparingTo("0");
        assertThat(resumen.desde()).isNull();
        assertThat(resumen.ventas()).isEmpty();
    }

    // --- helpers ---
    private ResumenCaja resumen() {
        return ResumenCaja.de(List.of(CierreCajaTestData.venta()));
    }

    private CierreCaja cerrar(ResumenCaja resumen, String revision, String fondo, String entradas,
            String retiros, String contado) {
        return CierreCaja.nuevo(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, "huella", resumen, revision, new BigDecimal(fondo),
                new BigDecimal(entradas), new BigDecimal(retiros),
                contado == null ? null : new BigDecimal(contado), "", CajaTestData.AHORA);
    }
}
