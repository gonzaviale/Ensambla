package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObtenerResumenDeLaCuentaResponseTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 14);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 12);
    private static final NegocioResumido ALMACEN = conDatos("a0000000-0000-4000-8000-000000000001",
            1250000, null, 840000, 60000, 3);
    private static final NegocioResumido KIOSCO = conDatos("a0000000-0000-4000-8000-000000000002",
            280000, 52000L, -72000, 0, 2);
    private static final NegocioResumido FERIA = new NegocioResumido(
            NegocioId.de("a0000000-0000-4000-8000-000000000003"), "Feria", RubroNegocio.OTRO, false, false,
            null, null, null, null, 1);

    @Test
    @DisplayName("Copia el período con su cantidad de días y los negocios en el mismo orden")
    void deShouldCopyThePeriodAndKeepTheOrderOfTheBusinesses() {
        ObtenerResumenDeLaCuentaResponse result = whenArmar(List.of(ALMACEN, KIOSCO, FERIA));

        assertThat(result.desde()).isEqualTo(DESDE);
        assertThat(result.hasta()).isEqualTo(HASTA);
        assertThat(result.dias()).isEqualTo(30);
        assertThat(result.negocios()).containsExactly(ALMACEN, KIOSCO, FERIA);
    }

    @Test
    @DisplayName("Los totales cuentan todos los negocios y suman sólo los importes que tienen valor")
    void deShouldCountEveryBusinessAndAddUpOnlyTheKnownAmounts() {
        ObtenerResumenDeLaCuentaResponse.Total total = whenArmar(List.of(ALMACEN, KIOSCO, FERIA)).total();

        assertThat(total.negocios()).isEqualTo(3);
        assertThat(total.vendiste()).isEqualByComparingTo("1530000");
        assertThat(total.gastos()).isEqualByComparingTo("52000");
        assertThat(total.queda()).isEqualByComparingTo("768000");
        assertThat(total.teDeben()).isEqualByComparingTo("60000");
        assertThat(total.avisosPendientes()).isEqualTo(6);
    }

    @Test
    @DisplayName("Sin datos en ningún negocio los importes del total quedan en cero")
    void deShouldReturnZeroAmountsWhenNoBusinessHasData() {
        ObtenerResumenDeLaCuentaResponse.Total total = whenArmar(List.of(FERIA)).total();

        assertThat(total.negocios()).isEqualTo(1);
        assertThat(total.vendiste()).isZero();
        assertThat(total.gastos()).isZero();
        assertThat(total.queda()).isZero();
        assertThat(total.teDeben()).isZero();
        assertThat(total.avisosPendientes()).isEqualTo(1);
    }

    // --- act ---
    private static ObtenerResumenDeLaCuentaResponse whenArmar(List<NegocioResumido> negocios) {
        return ObtenerResumenDeLaCuentaResponse.de(new PeriodoResumen(DESDE, HASTA), negocios);
    }

    // --- helpers ---
    private static NegocioResumido conDatos(
            String id, long vendiste, Long gastos, long queda, long teDeben, long avisosPendientes) {
        return new NegocioResumido(NegocioId.de(id), "Negocio " + id, RubroNegocio.OTRO, false, true,
                BigDecimal.valueOf(vendiste), gastos == null ? null : BigDecimal.valueOf(gastos),
                BigDecimal.valueOf(queda), BigDecimal.valueOf(teDeben), avisosPendientes);
    }
}
