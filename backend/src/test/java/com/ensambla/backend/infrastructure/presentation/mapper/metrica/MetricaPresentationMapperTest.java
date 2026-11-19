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
import com.ensambla.backend.infrastructure.presentation.dto.metrica.DefinicionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse.DeDondeSaleResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse.FilaResponse;
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
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;

class MetricaPresentationMapperTest {

    private static final DefinicionMetrica LAS_VENTAS_FIADAS_POR_PRODUCTO = unaReceta("Ventas del mes",
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true);

    private static final BigDecimal LO_DEL_PAN = new BigDecimal("750");

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 5);

    private static final LocalDate HASTA = LocalDate.of(2026, 11, 11);

    private static final ResultadoMetrica SETECIENTOS_CINCUENTA_DEL_PAN = ResultadoMetrica.conValor(
            FormatoMetrica.MONEDA, LO_DEL_PAN, List.of(new Fila("Pan", LO_DEL_PAN)),
            new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, DESDE, HASTA));

    private static final DefinicionResponse LA_RECETA_RESPONDIDA = new DefinicionResponse("Ventas del mes",
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true);

    private static final ResultadoResponse EL_RESULTADO_RESPONDIDO = new ResultadoResponse(FormatoMetrica.MONEDA,
            LO_DEL_PAN, List.of(new FilaResponse("Pan", LO_DEL_PAN)), null,
            new DeDondeSaleResponse(FuenteMetrica.VENTAS, 3, 1, DESDE, HASTA));

    private final MetricaPresentationMapper mapper = new MetricaPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listar las métricas con el usuario de la sesión y el cuaderno")
    void armaElPedidoDeListarLasMetricas() {
        entoncesPideListar(mapper.toListarRequest(USUARIO_ID, PROYECTO_ID),
                new ListarMetricasRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de listar las sugeridas con el usuario de la sesión y el cuaderno")
    void armaElPedidoDeListarLasSugeridas() {
        entoncesPideListar(mapper.toListarSugeridasRequest(USUARIO_ID, PROYECTO_ID),
                new ListarSugeridasRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de previsualizar con toda la receta")
    void armaElPedidoDePrevisualizarConTodaLaReceta() {
        entoncesPidePrevisualizar(new MetricaWebRequest("Ventas del mes", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true), true);
    }

    @Test
    @DisplayName("Si el pedido de previsualizar no dice si es fiada, mira todas las ventas")
    void sinDecirSiEsFiadaLaPrevisualizacionMiraTodasLasVentas() {
        entoncesPidePrevisualizar(new MetricaWebRequest("Ventas del mes", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, null), false);
    }

    @Test
    @DisplayName("Arma el pedido de crear con toda la receta y la autoría")
    void armaElPedidoDeCrearConTodaLaRecetaYLaAutoria() {
        entoncesPideCrear(new CrearMetricaWebRequest("Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true, AutoriaMetrica.PROPIA), true);
    }

    @Test
    @DisplayName("Si el pedido de crear no dice si es fiada, mira todas las ventas")
    void sinDecirSiEsFiadaLaNuevaMiraTodasLasVentas() {
        entoncesPideCrear(new CrearMetricaWebRequest("Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, null, AutoriaMetrica.PROPIA), false);
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario, el cuaderno y la métrica")
    void armaElPedidoDeEliminar() {
        entoncesPideEliminar(new EliminarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID));
    }

    @Test
    @DisplayName("Responde las métricas con su lugar, su autoría, su receta y su resultado completo")
    void respondeLasMetricasConTodosSusDatos() {
        entoncesRespondeLasMetricas(List.of(MetricaItem.de(metrica(LAS_VENTAS_FIADAS_POR_PRODUCTO, 2),
                        SETECIENTOS_CINCUENTA_DEL_PAN)),
                new MetricaResponse(METRICA_ID, LA_RECETA_RESPONDIDA, 2, AutoriaMetrica.SUGERIDA,
                        EL_RESULTADO_RESPONDIDO));
    }

    @Test
    @DisplayName("Responde el resultado con su reparto y de dónde sale")
    void respondeElResultadoConSuRepartoYDeDondeSale() {
        entoncesRespondeElResultado(ResultadoItem.de(SETECIENTOS_CINCUENTA_DEL_PAN), EL_RESULTADO_RESPONDIDO);
    }

    @Test
    @DisplayName("Responde las sugeridas con su receta y su resultado")
    void respondeLasSugeridasConSuRecetaYSuResultado() {
        entoncesRespondeLasSugeridas(List.of(new SugeridaItem(DefinicionItem.de(LAS_VENTAS_FIADAS_POR_PRODUCTO),
                        ResultadoItem.de(SETECIENTOS_CINCUENTA_DEL_PAN))),
                new SugeridaResponse(LA_RECETA_RESPONDIDA, EL_RESULTADO_RESPONDIDO));
    }

    @Test
    @DisplayName("Sin métricas responde una lista vacía")
    void sinMetricasRespondeUnaListaVacia() {
        entoncesRespondeLasMetricas(List.of());
    }

    @Test
    @DisplayName("Sin sugeridas responde una lista vacía")
    void sinSugeridasRespondeUnaListaVacia() {
        entoncesRespondeLasSugeridas(List.of());
    }

    private static void entoncesPideListar(Object pedido, Object esperado) {
        assertThat(pedido).isEqualTo(esperado);
    }

    private void entoncesPidePrevisualizar(MetricaWebRequest web, boolean soloFiado) {
        assertThat(mapper.toPrevisualizarRequest(USUARIO_ID, PROYECTO_ID, web)).isEqualTo(
                new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas del mes", FuenteMetrica.VENTAS,
                        CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, soloFiado));
    }

    private void entoncesPideCrear(CrearMetricaWebRequest web, boolean soloFiado) {
        assertThat(mapper.toCrearRequest(USUARIO_ID, PROYECTO_ID, web)).isEqualTo(
                new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas del mes", FuenteMetrica.VENTAS,
                        CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, soloFiado,
                        AutoriaMetrica.PROPIA));
    }

    private void entoncesPideEliminar(EliminarMetricaRequest esperado) {
        assertThat(mapper.toEliminarRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID)).isEqualTo(esperado);
    }

    private void entoncesRespondeLasMetricas(List<MetricaItem> metricas, MetricaResponse... esperadas) {
        assertThat(mapper.toResponse(metricas)).containsExactly(esperadas);
    }

    private void entoncesRespondeElResultado(ResultadoItem resultado, ResultadoResponse esperado) {
        assertThat(mapper.toResponse(resultado)).isEqualTo(esperado);
    }

    private void entoncesRespondeLasSugeridas(List<SugeridaItem> sugeridas, SugeridaResponse... esperadas) {
        assertThat(mapper.toSugeridasResponse(sugeridas)).containsExactly(esperadas);
    }
}
