package com.ensambla.backend.domain.model.aviso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo que cumple toda parte del resumen de la mañana; cada una la hereda y suma lo propio. */
abstract class SeccionResumenContratoTest {

    protected abstract SeccionResumen crear();

    /** Un aviso pendiente de los que esa parte cuenta. */
    protected abstract Aviso avisoQueCuenta();

    /** Un aviso pendiente de los que esa parte no cuenta. */
    protected abstract Aviso avisoQueNoCuenta();

    @Test
    @DisplayName("Sin avisos no dice nada")
    void textoShouldBeEmptyWhenThereAreNoAvisos() {
        Optional<String> result = crear().texto(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Con avisos de otro tipo no dice nada")
    void textoShouldBeEmptyWhenNoAvisoBelongsToIt() {
        Optional<String> result = crear().texto(List.of(avisoQueNoCuenta()));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Con un aviso suyo dice cuántos hay")
    void textoShouldCountTheAvisosThatBelongToIt() {
        Optional<String> result = crear().texto(List.of(avisoQueNoCuenta(), avisoQueCuenta()));

        assertThat(result).hasValueSatisfying(texto -> assertThat(texto).startsWith("1 "));
    }

    @Test
    @DisplayName("Con varios avisos suyos los suma")
    void textoShouldSumSeveralAvisos() {
        Optional<String> result = crear().texto(List.of(avisoQueCuenta(), avisoQueCuenta(), avisoQueCuenta()));

        assertThat(result).hasValueSatisfying(texto -> assertThat(texto).startsWith("3 "));
    }

    protected static Aviso aviso(TipoAviso tipo, Number cantidad) {
        return Aviso.builder()
                .tipo(tipo)
                .grupo(tipo.grupo())
                .estado(EstadoAviso.PENDIENTE)
                .datos(Map.of("cantidad", cantidad))
                .build();
    }
}
