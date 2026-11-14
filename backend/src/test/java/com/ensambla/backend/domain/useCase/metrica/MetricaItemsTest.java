package com.ensambla.backend.domain.useCase.metrica;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static org.assertj.core.api.Assertions.assertThat;

class MetricaItemsTest {

    private static final DefinicionMetrica DEFINICION = new DefinicionMetrica(
            "Ventas por producto", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO,
            PeriodoMetrica.SEMANA, true);
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 5);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 11);

    @Test
    @DisplayName("La receta copia todos sus datos")
    void definicionItemShouldCopyAllFields() {
        DefinicionItem result = DefinicionItem.de(DEFINICION);

        assertThat(result.nombre()).isEqualTo("Ventas por producto");
        assertThat(result.fuente()).isEqualTo(FuenteMetrica.VENTAS);
        assertThat(result.calculo()).isEqualTo(CalculoMetrica.SUMA);
        assertThat(result.agrupar()).isEqualTo(AgrupacionMetrica.PRODUCTO);
        assertThat(result.periodo()).isEqualTo(PeriodoMetrica.SEMANA);
        assertThat(result.soloFiado()).isTrue();
    }

    @Test
    @DisplayName("El resultado copia el número, el reparto y de dónde sale")
    void resultadoItemShouldCopyAllFields() {
        ResultadoMetrica resultado = ResultadoMetrica.conValor(
                FormatoMetrica.MONEDA, new BigDecimal("750"), List.of(new Fila("Pan", new BigDecimal("750"))),
                new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, DESDE, HASTA));

        ResultadoItem result = ResultadoItem.de(resultado);

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
        assertThat(result.deDondeSale().desde()).isEqualTo(DESDE);
        assertThat(result.deDondeSale().hasta()).isEqualTo(HASTA);
    }

    @Test
    @DisplayName("El resultado sin número conserva el motivo")
    void resultadoItemShouldKeepTheReason() {
        ResultadoMetrica resultado = ResultadoMetrica.sinValor(
                FormatoMetrica.NUMERO, "No hay ventas en este período.",
                new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null));

        ResultadoItem result = ResultadoItem.de(resultado);

        assertThat(result.valor()).isNull();
        assertThat(result.motivo()).isEqualTo("No hay ventas en este período.");
        assertThat(result.filas()).isEmpty();
        assertThat(result.deDondeSale().desde()).isNull();
    }

    @Test
    @DisplayName("La métrica guardada copia su id, su lugar, su autoría, su receta y su resultado")
    void metricaItemShouldCopyAllFields() {
        ResultadoMetrica resultado = ResultadoMetrica.sinValor(
                FormatoMetrica.MONEDA, "motivo", new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null));

        MetricaItem result = MetricaItem.de(metrica(DEFINICION, 4), resultado);

        assertThat(result.id()).isEqualTo(METRICA_ID);
        assertThat(result.orden()).isEqualTo(4);
        assertThat(result.autoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(result.definicion().nombre()).isEqualTo("Ventas por producto");
        assertThat(result.resultado().motivo()).isEqualTo("motivo");
    }

    @Test
    @DisplayName("La sugerida copia su receta y su resultado")
    void sugeridaItemShouldCopyAllFields() {
        ResultadoMetrica resultado = ResultadoMetrica.sinValor(
                FormatoMetrica.MONEDA, "motivo", new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null));

        SugeridaItem result = SugeridaItem.de(DEFINICION, resultado);

        assertThat(result.definicion().calculo()).isEqualTo(CalculoMetrica.SUMA);
        assertThat(result.resultado().motivo()).isEqualTo("motivo");
    }
}
