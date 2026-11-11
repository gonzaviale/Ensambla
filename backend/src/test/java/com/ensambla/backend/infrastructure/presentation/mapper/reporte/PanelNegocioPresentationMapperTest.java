package com.ensambla.backend.infrastructure.presentation.mapper.reporte;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocioResponse;
import com.ensambla.backend.infrastructure.presentation.dto.reporte.PanelNegocioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PanelNegocioPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 7);

    private final PanelNegocioPresentationMapper mapper = new PanelNegocioPresentationMapper();

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, el sistema y los días")
    void toRequestShouldBuildTheRequest() {
        assertThat(mapper.toRequest(USUARIO_ID, PROYECTO_ID, 7))
                .isEqualTo(new ObtenerPanelNegocioRequest(USUARIO_ID, PROYECTO_ID, 7));
    }

    @Test
    @DisplayName("Pasa las métricas y las semanas a la respuesta HTTP")
    void toResponseShouldCopyMetricsAndWeeks() {
        ObtenerPanelNegocioResponse response = new ObtenerPanelNegocioResponse(DESDE, HASTA, 7, true,
                new ObtenerPanelNegocioResponse.Metricas(
                        bd("1000"), bd("200"), bd("100"), bd("700"), bd("50"), bd("20.5"), bd("250"), bd("10")),
                List.of(new ObtenerPanelNegocioResponse.Semana(
                        DESDE, HASTA, bd("1000"), bd("300"), bd("700"), bd("700"))));

        PanelNegocioResponse web = mapper.toResponse(response);

        assertThat(web.desde()).isEqualTo(DESDE);
        assertThat(web.hasta()).isEqualTo(HASTA);
        assertThat(web.dias()).isEqualTo(7);
        assertThat(web.hayDatos()).isTrue();
        assertThat(web.metricas()).isEqualTo(new PanelNegocioResponse.Metricas(
                bd("1000"), bd("200"), bd("100"), bd("700"), bd("50"), bd("20.5"), bd("250"), bd("10")));
        assertThat(web.flujoDeCaja()).containsExactly(
                new PanelNegocioResponse.Semana(DESDE, HASTA, bd("1000"), bd("300"), bd("700"), bd("700")));
    }

    @Test
    @DisplayName("Una respuesta vacía queda sin métricas ni semanas")
    void toResponseShouldKeepAnEmptyPanelEmpty() {
        PanelNegocioResponse web = mapper.toResponse(
                new ObtenerPanelNegocioResponse(DESDE, HASTA, 7, false, null, List.of()));

        assertThat(web.hayDatos()).isFalse();
        assertThat(web.metricas()).isNull();
        assertThat(web.flujoDeCaja()).isEmpty();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
