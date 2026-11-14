package com.ensambla.backend.infrastructure.presentation.mapper.metrica;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DeDondeSale;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FormatoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricasRequest;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridasRequest;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetricaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.CrearMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.SugeridaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static org.assertj.core.api.Assertions.assertThat;

class MetricaPresentationMapperTest {

    private static final DefinicionMetrica DEFINICION = new DefinicionMetrica(
            "Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO,
            PeriodoMetrica.MES, true);

    private final MetricaPresentationMapper mapper = new MetricaPresentationMapper();

    @Test
    @DisplayName("Arma los pedidos de listar con el usuario de la sesión y el cuaderno")
    void toListarRequestsShouldCarryUserAndProject() {
        assertThat(mapper.toListarRequest(USUARIO_ID, PROYECTO_ID))
                .isEqualTo(new ListarMetricasRequest(USUARIO_ID, PROYECTO_ID));
        assertThat(mapper.toListarSugeridasRequest(USUARIO_ID, PROYECTO_ID))
                .isEqualTo(new ListarSugeridasRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de previsualizar con toda la receta")
    void toPrevisualizarRequestShouldCopyTheRecipe() {
        MetricaWebRequest web = new MetricaWebRequest("Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true);

        PrevisualizarMetricaRequest result = mapper.toPrevisualizarRequest(USUARIO_ID, PROYECTO_ID, web);

        assertThat(result).isEqualTo(new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas del mes",
                FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true));
    }

    @Test
    @DisplayName("Arma el pedido de crear con toda la receta y la autoría")
    void toCrearRequestShouldCopyTheRecipeAndAuthorship() {
        CrearMetricaWebRequest web = new CrearMetricaWebRequest("Ventas del mes", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true, AutoriaMetrica.PROPIA);

        CrearMetricaRequest result = mapper.toCrearRequest(USUARIO_ID, PROYECTO_ID, web);

        assertThat(result).isEqualTo(new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas del mes",
                FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true,
                AutoriaMetrica.PROPIA));
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario, el cuaderno y la métrica")
    void toEliminarRequestShouldCarryTheIds() {
        assertThat(mapper.toEliminarRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID))
                .isEqualTo(new EliminarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID));
    }

    @Test
    @DisplayName("Mapea las métricas con su receta y su resultado completo")
    void toResponseShouldMapMetricsEveryField() {
        MetricaItem item = MetricaItem.de(metrica(DEFINICION, 2), resultado());

        List<MetricaResponse> result = mapper.toResponse(List.of(item));

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(METRICA_ID);
            assertThat(response.orden()).isEqualTo(2);
            assertThat(response.autoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
            assertThat(response.definicion().nombre()).isEqualTo("Ventas del mes");
            assertThat(response.definicion().fuente()).isEqualTo(FuenteMetrica.VENTAS);
            assertThat(response.definicion().calculo()).isEqualTo(CalculoMetrica.SUMA);
            assertThat(response.definicion().agrupar()).isEqualTo(AgrupacionMetrica.PRODUCTO);
            assertThat(response.definicion().periodo()).isEqualTo(PeriodoMetrica.MES);
            assertThat(response.definicion().soloFiado()).isTrue();
            assertThat(response.resultado().valor()).isEqualByComparingTo("750");
        });
    }

    @Test
    @DisplayName("Mapea el resultado con su reparto y de dónde sale")
    void toResponseShouldMapResultEveryField() {
        ResultadoResponse result = mapper.toResponse(ResultadoItem.de(resultado()));

        assertThat(result.formato()).isEqualTo(FormatoMetrica.MONEDA);
        assertThat(result.valor()).isEqualByComparingTo("750");
        assertThat(result.motivo()).isNull();
        assertThat(result.filas()).singleElement().satisfies(fila -> {
            assertThat(fila.etiqueta()).isEqualTo("Pan");
            assertThat(fila.valor()).isEqualByComparingTo("750");
        });
        assertThat(result.deDondeSale().fuente()).isEqualTo(FuenteMetrica.VENTAS);
        assertThat(result.deDondeSale().filasUsadas()).isEqualTo(3);
        assertThat(result.deDondeSale().filasSinDato()).isEqualTo(1);
        assertThat(result.deDondeSale().desde()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(result.deDondeSale().hasta()).isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    @DisplayName("Mapea las sugeridas con su receta y su resultado")
    void toSugeridasResponseShouldMapSuggestions() {
        SugeridaItem sugerida = new SugeridaItem(DefinicionItem.de(DEFINICION), ResultadoItem.de(resultado()));

        List<SugeridaResponse> result = mapper.toSugeridasResponse(List.of(sugerida));

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.definicion().nombre()).isEqualTo("Ventas del mes");
            assertThat(response.resultado().valor()).isEqualByComparingTo("750");
        });
    }

    @Test
    @DisplayName("Sin métricas ni sugeridas devuelve listas vacías")
    void toResponseShouldReturnEmptyLists() {
        assertThat(mapper.toResponse(List.<MetricaItem>of())).isEmpty();
        assertThat(mapper.toSugeridasResponse(List.of())).isEmpty();
    }

    // --- helpers ---
    private static ResultadoMetrica resultado() {
        return ResultadoMetrica.conValor(FormatoMetrica.MONEDA, new BigDecimal("750"),
                List.of(new Fila("Pan", new BigDecimal("750"))),
                new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 11)));
    }
}
