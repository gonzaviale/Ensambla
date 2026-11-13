package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PanelNegocio.Metricas;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NegocioResumidoTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final String NOMBRE = "Kiosco de la esquina";
    private static final long AVISOS_PENDIENTES = 2;
    private static final PeriodoResumen PERIODO =
            new PeriodoResumen(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 11, 12));
    private static final PanelNegocio SIN_VENTAS = new PanelNegocio(PERIODO, null, List.of());

    @Test
    @DisplayName("Copia los datos del negocio, si es el activo y sus avisos pendientes")
    void deShouldCopyTheBusinessData() {
        NegocioResumido result = NegocioResumido.de(negocio(), true, List.of(), AVISOS_PENDIENTES);

        assertThat(result.id()).isEqualTo(NEGOCIO_ID);
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.rubro()).isEqualTo(RubroNegocio.KIOSCO);
        assertThat(result.activo()).isTrue();
        assertThat(result.avisosPendientes()).isEqualTo(AVISOS_PENDIENTES);
    }

    @Test
    @DisplayName("Suma las métricas de los sistemas con ventas y deja afuera los que no tienen")
    void deShouldAddUpTheMetricsOfTheSystemsWithSales() {
        List<PanelNegocio> paneles = List.of(
                panel(metricas(280000, 52000, -72000, 15000)),
                SIN_VENTAS,
                panel(metricas(120000, 8000, 90000, 0)));

        NegocioResumido result = NegocioResumido.de(negocio(), false, paneles, AVISOS_PENDIENTES);

        assertThat(result.hayDatos()).isTrue();
        assertThat(result.vendiste()).isEqualByComparingTo("400000");
        assertThat(result.gastos()).isEqualByComparingTo("60000");
        assertThat(result.queda()).isEqualByComparingTo("18000");
        assertThat(result.teDeben()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("Sin ventas en el período no hay datos y las métricas quedan en null, no en cero")
    void deShouldLeaveTheMetricsNullWhenNoSystemHasSales() {
        NegocioResumido result = NegocioResumido.de(negocio(), false, List.of(SIN_VENTAS), AVISOS_PENDIENTES);

        assertThat(result.hayDatos()).isFalse();
        assertThat(result.vendiste()).isNull();
        assertThat(result.gastos()).isNull();
        assertThat(result.queda()).isNull();
        assertThat(result.teDeben()).isNull();
        assertThat(result.avisosPendientes()).isEqualTo(AVISOS_PENDIENTES);
    }

    @Test
    @DisplayName("Una métrica que ningún sistema puede calcular queda en null; si alguno la tiene, suma sólo esa")
    void deShouldOnlyAddTheMetricWhereItIsKnown() {
        Metricas sinGastos = new Metricas(BigDecimal.valueOf(50000), null, null, BigDecimal.valueOf(50000),
                BigDecimal.ZERO, null, null, null);

        NegocioResumido sinNinguno = NegocioResumido.de(negocio(), false, List.of(panel(sinGastos)), 0);
        NegocioResumido conUno = NegocioResumido.de(
                negocio(), false, List.of(panel(sinGastos), panel(metricas(10000, 4000, 6000, 0))), 0);

        assertThat(sinNinguno.gastos()).isNull();
        assertThat(conUno.gastos()).isEqualByComparingTo("4000");
    }

    // --- helpers ---
    private static Negocio negocio() {
        return Negocio.builder()
                .id(NEGOCIO_ID)
                .nombre(NOMBRE)
                .rubro(RubroNegocio.KIOSCO)
                .build();
    }

    private static PanelNegocio panel(Metricas metricas) {
        return new PanelNegocio(PERIODO, metricas, List.of());
    }

    private static Metricas metricas(long vendiste, long gastos, long queda, long teDeben) {
        return new Metricas(BigDecimal.valueOf(vendiste), null, BigDecimal.valueOf(gastos), BigDecimal.valueOf(queda),
                BigDecimal.valueOf(teDeben), null, null, null);
    }
}
