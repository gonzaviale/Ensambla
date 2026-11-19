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

    private PanelNegocio panel;
    private ObtenerPanelNegocioResponse respuesta;

    @Test
    @DisplayName("Copia el período, las métricas y cada semana del panel")
    void copiaElPeriodoLasMetricasYCadaSemanaDelPanel() {
        dadoUnPanelConMetricasYUnaSemana();

        cuandoSeArmaLaRespuesta();

        entoncesCopiaElPeriodoLasMetricasYLaSemana();
    }

    @Test
    @DisplayName("Un panel vacío no tiene datos, métricas ni semanas")
    void unPanelVacioNoTieneDatosMetricasNiSemanas() {
        dadoUnPanelVacio();

        cuandoSeArmaLaRespuesta();

        entoncesNoTieneDatosNiMetricasNiSemanas();
    }

    // --- dado ---
    private void dadoUnPanelConMetricasYUnaSemana() {
        PanelNegocio.Metricas metricas = new PanelNegocio.Metricas(
                bd("1000"), bd("200"), null, bd("800"), bd("50"), bd("20.5"), bd("250"), null);
        PanelNegocio.SemanaDeCaja semana = new PanelNegocio.SemanaDeCaja(
                PERIODO.desde(), PERIODO.hasta(), bd("1000"), bd("200"), bd("800"), bd("800"));
        panel = new PanelNegocio(PERIODO, metricas, List.of(semana));
    }

    private void dadoUnPanelVacio() {
        panel = new PanelNegocio(PERIODO, null, List.of());
    }

    // --- cuando ---
    private void cuandoSeArmaLaRespuesta() {
        respuesta = ObtenerPanelNegocioResponse.de(panel);
    }

    // --- entonces ---
    private void entoncesCopiaElPeriodoLasMetricasYLaSemana() {
        assertThat(respuesta.desde()).isEqualTo(PERIODO.desde());
        assertThat(respuesta.hasta()).isEqualTo(PERIODO.hasta());
        assertThat(respuesta.dias()).isEqualTo(7);
        assertThat(respuesta.hayDatos()).isTrue();
        assertThat(respuesta.metricas()).isEqualTo(new ObtenerPanelNegocioResponse.Metricas(
                bd("1000"), bd("200"), null, bd("800"), bd("50"), bd("20.5"), bd("250"), null));
        assertThat(respuesta.flujoDeCaja()).containsExactly(new ObtenerPanelNegocioResponse.Semana(
                PERIODO.desde(), PERIODO.hasta(), bd("1000"), bd("200"), bd("800"), bd("800")));
    }

    private void entoncesNoTieneDatosNiMetricasNiSemanas() {
        assertThat(respuesta.hayDatos()).isFalse();
        assertThat(respuesta.metricas()).isNull();
        assertThat(respuesta.flujoDeCaja()).isEmpty();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
