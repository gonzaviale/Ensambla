package com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio;

import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObtenerPanelNegocioResponseTest {

    private static final PeriodoResumen PERIODO =
            new PeriodoResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));

    @Test
    @DisplayName("Copia el período, las métricas y cada semana del panel")
    void deShouldCopyThePanel() {
        PanelNegocio.Metricas metricas = new PanelNegocio.Metricas(
                bd("1000"), bd("200"), null, bd("800"), bd("50"), bd("20.5"), bd("250"), null);
        PanelNegocio.SemanaDeCaja semana = new PanelNegocio.SemanaDeCaja(
                PERIODO.desde(), PERIODO.hasta(), bd("1000"), bd("200"), bd("800"), bd("800"));

        ObtenerPanelNegocioResponse response = ObtenerPanelNegocioResponse.de(
                new PanelNegocio(PERIODO, metricas, List.of(semana)));

        assertThat(response.desde()).isEqualTo(PERIODO.desde());
        assertThat(response.hasta()).isEqualTo(PERIODO.hasta());
        assertThat(response.dias()).isEqualTo(7);
        assertThat(response.hayDatos()).isTrue();
        assertThat(response.metricas()).isEqualTo(new ObtenerPanelNegocioResponse.Metricas(
                bd("1000"), bd("200"), null, bd("800"), bd("50"), bd("20.5"), bd("250"), null));
        assertThat(response.flujoDeCaja()).containsExactly(new ObtenerPanelNegocioResponse.Semana(
                PERIODO.desde(), PERIODO.hasta(), bd("1000"), bd("200"), bd("800"), bd("800")));
    }

    @Test
    @DisplayName("Un panel vacío no tiene datos, métricas ni semanas")
    void deShouldMarkAnEmptyPanel() {
        ObtenerPanelNegocioResponse response = ObtenerPanelNegocioResponse.de(
                new PanelNegocio(PERIODO, null, List.of()));

        assertThat(response.hayDatos()).isFalse();
        assertThat(response.metricas()).isNull();
        assertThat(response.flujoDeCaja()).isEmpty();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
