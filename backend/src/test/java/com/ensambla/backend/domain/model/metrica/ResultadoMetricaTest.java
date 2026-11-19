package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoMetricaTest {

    private static final DeDondeSale ORIGEN = new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, null, null);

    private static final List<Fila> LO_DEL_PAN = List.of(new Fila("Pan", BigDecimal.ONE));

    private ResultadoMetrica resultado;

    @Test
    @DisplayName("Un resultado con valor es calculable y no trae motivo")
    void unResultadoConValorEsCalculable() {
        cuandoDaDiezConElRepartoDelPan();

        entoncesEsCalculableYDa(BigDecimal.TEN);
        entoncesSeRepartePor(LO_DEL_PAN);
    }

    @Test
    @DisplayName("Un resultado sin valor explica el motivo y no tiene reparto")
    void unResultadoSinValorExplicaElMotivo() {
        cuandoNoSePuedeCalcularPorque("No hay datos");

        entoncesNoEsCalculablePorque("No hay datos");
        entoncesSeRepartePor(List.of());
    }

    @Test
    @DisplayName("Sin reparto la lista queda vacía")
    void sinRepartoLaListaQuedaVacia() {
        cuandoDaUnoSinReparto();

        entoncesSeRepartePor(List.of());
    }

    private void cuandoDaDiezConElRepartoDelPan() {
        resultado = ResultadoMetrica.conValor(FormatoMetrica.MONEDA, BigDecimal.TEN, LO_DEL_PAN, ORIGEN);
    }

    private void cuandoNoSePuedeCalcularPorque(String motivo) {
        resultado = ResultadoMetrica.sinValor(FormatoMetrica.NUMERO, motivo, ORIGEN);
    }

    private void cuandoDaUnoSinReparto() {
        resultado = new ResultadoMetrica(FormatoMetrica.NUMERO, BigDecimal.ONE, null, null, ORIGEN);
    }

    private void entoncesEsCalculableYDa(BigDecimal valor) {
        assertThat(resultado.calculable()).isTrue();
        assertThat(resultado.valor()).isEqualTo(valor);
        assertThat(resultado.motivo()).isNull();
    }

    private void entoncesNoEsCalculablePorque(String motivo) {
        assertThat(resultado.calculable()).isFalse();
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.motivo()).isEqualTo(motivo);
    }

    private void entoncesSeRepartePor(List<Fila> filas) {
        assertThat(resultado.filas()).isEqualTo(filas);
    }
}
