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
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem.DeDondeSaleItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem.FilaItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;

class MetricaItemsTest {

    private static final DefinicionMetrica LAS_VENTAS_FIADAS_POR_PRODUCTO = unaReceta("Ventas por producto",
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.SEMANA, true);

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 5);

    private static final LocalDate HASTA = LocalDate.of(2026, 11, 11);

    private static final BigDecimal LO_DEL_PAN = new BigDecimal("750");

    private static final ResultadoMetrica SETECIENTOS_CINCUENTA_DEL_PAN = ResultadoMetrica.conValor(
            FormatoMetrica.MONEDA, LO_DEL_PAN, List.of(new Fila("Pan", LO_DEL_PAN)),
            new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, DESDE, HASTA));

    private static final ResultadoMetrica SIN_VENTAS_EN_EL_PERIODO = ResultadoMetrica.sinValor(
            FormatoMetrica.NUMERO, "No hay ventas en este período.",
            new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null));

    private DefinicionItem receta;

    private ResultadoItem resultado;

    private MetricaItem guardada;

    private SugeridaItem sugerida;

    @Test
    @DisplayName("La receta copia todos sus datos")
    void laRecetaCopiaTodosSusDatos() {
        cuandoSeCopiaLaReceta(LAS_VENTAS_FIADAS_POR_PRODUCTO);

        entoncesLaRecetaEsLaDeLasVentasFiadasPorProducto();
    }

    @Test
    @DisplayName("El resultado copia el número, el reparto y de dónde sale")
    void elResultadoCopiaElNumeroElRepartoYDeDondeSale() {
        cuandoSeCopiaElResultado(SETECIENTOS_CINCUENTA_DEL_PAN);

        entoncesElResultadoDa(LO_DEL_PAN, List.of(new FilaItem("Pan", LO_DEL_PAN)));
        entoncesSaleDe(new DeDondeSaleItem(FuenteMetrica.VENTAS, 3, 1, DESDE, HASTA));
    }

    @Test
    @DisplayName("El resultado sin número conserva el motivo")
    void elResultadoSinNumeroConservaElMotivo() {
        cuandoSeCopiaElResultado(SIN_VENTAS_EN_EL_PERIODO);

        entoncesElResultadoNoDaPorque("No hay ventas en este período.");
        entoncesSaleDe(new DeDondeSaleItem(FuenteMetrica.VENTAS, 0, 0, null, null));
    }

    @Test
    @DisplayName("La métrica guardada copia su id, su lugar, su autoría, su receta y su resultado")
    void laMetricaGuardadaCopiaTodo() {
        cuandoSeCopiaLaMetricaGuardadaEnElLugar(4);

        entoncesLaGuardadaEstaEnElLugar(4);
    }

    @Test
    @DisplayName("La sugerida copia su receta y su resultado")
    void laSugeridaCopiaSuRecetaYSuResultado() {
        cuandoSeCopiaLaSugerida();

        entoncesLaSugeridaTraeSuRecetaYSuResultado();
    }

    private void cuandoSeCopiaLaReceta(DefinicionMetrica definicion) {
        receta = DefinicionItem.de(definicion);
    }

    private void cuandoSeCopiaElResultado(ResultadoMetrica calculado) {
        resultado = ResultadoItem.de(calculado);
    }

    private void cuandoSeCopiaLaMetricaGuardadaEnElLugar(int orden) {
        guardada = MetricaItem.de(metrica(LAS_VENTAS_FIADAS_POR_PRODUCTO, orden), SIN_VENTAS_EN_EL_PERIODO);
    }

    private void cuandoSeCopiaLaSugerida() {
        sugerida = SugeridaItem.de(LAS_VENTAS_FIADAS_POR_PRODUCTO, SIN_VENTAS_EN_EL_PERIODO);
    }

    private void entoncesLaRecetaEsLaDeLasVentasFiadasPorProducto() {
        assertThat(receta).isEqualTo(new DefinicionItem("Ventas por producto", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.SEMANA, true));
    }

    private void entoncesElResultadoDa(BigDecimal valor, List<FilaItem> filas) {
        assertThat(resultado.formato()).isEqualTo(FormatoMetrica.MONEDA);
        assertThat(resultado.valor()).isEqualByComparingTo(valor);
        assertThat(resultado.motivo()).isNull();
        assertThat(resultado.filas()).isEqualTo(filas);
    }

    private void entoncesElResultadoNoDaPorque(String motivo) {
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.motivo()).isEqualTo(motivo);
        assertThat(resultado.filas()).isEmpty();
    }

    private void entoncesSaleDe(DeDondeSaleItem deDondeSale) {
        assertThat(resultado.deDondeSale()).isEqualTo(deDondeSale);
    }

    private void entoncesLaGuardadaEstaEnElLugar(int orden) {
        assertThat(guardada.id()).isEqualTo(METRICA_ID);
        assertThat(guardada.orden()).isEqualTo(orden);
        assertThat(guardada.autoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(guardada.definicion()).isEqualTo(DefinicionItem.de(LAS_VENTAS_FIADAS_POR_PRODUCTO));
        assertThat(guardada.resultado()).isEqualTo(ResultadoItem.de(SIN_VENTAS_EN_EL_PERIODO));
    }

    private void entoncesLaSugeridaTraeSuRecetaYSuResultado() {
        assertThat(sugerida.definicion()).isEqualTo(DefinicionItem.de(LAS_VENTAS_FIADAS_POR_PRODUCTO));
        assertThat(sugerida.resultado()).isEqualTo(ResultadoItem.de(SIN_VENTAS_EN_EL_PERIODO));
    }
}
