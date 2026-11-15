package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo que tiene que cumplir todo tipo de campo; cada subclase aporta solo sus valores y su mensaje. */
abstract class CampoPorTipoTest {

    protected abstract TipoCampo tipoEsperado();

    protected abstract List<Object> valoresValidos();

    protected abstract List<Object> valoresInvalidos();

    protected abstract String mensajeDeError();

    @Test
    @DisplayName("El campo es del tipo que le corresponde")
    void tipoShouldBeTheExpectedOne() {
        assertThat(campo().tipo()).isEqualTo(tipoEsperado());
        assertThat(campo().getTipo()).isEqualTo(tipoEsperado());
    }

    @Test
    @DisplayName("Acepta los valores que corresponden a su tipo")
    void errorDeShouldBeEmptyWhenValorMatchesTipo() {
        assertThat(valoresValidos()).allSatisfy(valor -> assertThat(campo().errorDe(valor)).isEmpty());
    }

    @Test
    @DisplayName("Rechaza los valores que no corresponden a su tipo, con su mensaje")
    void errorDeShouldReportWhenValorDoesNotMatchTipo() {
        assertThat(valoresInvalidos())
                .allSatisfy(valor -> assertThat(campo().errorDe(valor)).contains(mensajeDeError()));
    }

    protected Campo campo() {
        return Campo.builder()
                .tipo(tipoEsperado())
                .opciones(List.of("Minorista", "Mayorista"))
                .build();
    }
}
