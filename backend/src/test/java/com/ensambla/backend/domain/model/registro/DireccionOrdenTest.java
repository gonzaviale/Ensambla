package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DireccionOrdenTest {

    private DireccionOrden direccion;
    private Throwable rechazo;

    @Test
    @DisplayName("Sin valor ordena de forma descendente")
    void sinValorOrdenaDeFormaDescendente() {
        cuandoSeInterpreta(null);
        entoncesLaDireccionEs(DireccionOrden.DESC);

        cuandoSeInterpreta("  ");
        entoncesLaDireccionEs(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Interpreta asc y desc sin distinguir mayúsculas ni espacios")
    void interpretaAscYDescSinDistinguirMayusculasNiEspacios() {
        cuandoSeInterpreta("ASC");
        entoncesLaDireccionEs(DireccionOrden.ASC);

        cuandoSeInterpreta(" desc ");
        entoncesLaDireccionEs(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Rechaza cualquier otro valor")
    void rechazaCualquierOtroValor() {
        cuandoSeIntentaInterpretar("arriba");

        entoncesSeRechazaPorDireccionInvalida();
    }

    // --- cuando ---
    private void cuandoSeInterpreta(String valor) {
        direccion = DireccionOrden.desde(valor);
    }

    private void cuandoSeIntentaInterpretar(String valor) {
        rechazo = catchThrowable(() -> cuandoSeInterpreta(valor));
    }

    // --- entonces ---
    private void entoncesLaDireccionEs(DireccionOrden esperada) {
        assertThat(direccion).isEqualTo(esperada);
    }

    private void entoncesSeRechazaPorDireccionInvalida() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La dirección del orden tiene que ser asc o desc");
    }
}
