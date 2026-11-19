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
    void sinValorSeOrdenaDeFormaDescendente() {
        cuandoSeInterpretaElValor(null);

        entoncesLaDireccionEs(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Un valor en blanco ordena de forma descendente")
    void unValorEnBlancoSeOrdenaDeFormaDescendente() {
        cuandoSeInterpretaElValor("  ");

        entoncesLaDireccionEs(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Interpreta asc sin distinguir mayúsculas")
    void ascSeInterpretaSinDistinguirMayusculas() {
        cuandoSeInterpretaElValor("ASC");

        entoncesLaDireccionEs(DireccionOrden.ASC);
    }

    @Test
    @DisplayName("Interpreta desc sin distinguir espacios")
    void descSeInterpretaSinDistinguirEspacios() {
        cuandoSeInterpretaElValor(" desc ");

        entoncesLaDireccionEs(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Rechaza cualquier otro valor")
    void unValorDesconocidoSeRechaza() {
        cuandoSeIntentaInterpretarElValor("arriba");

        entoncesSeRechazaPorque("La dirección del orden tiene que ser asc o desc");
    }

    // --- cuando ---
    private void cuandoSeInterpretaElValor(String valor) {
        direccion = DireccionOrden.desde(valor);
    }

    private void cuandoSeIntentaInterpretarElValor(String valor) {
        rechazo = catchThrowable(() -> DireccionOrden.desde(valor));
    }

    // --- entonces ---
    private void entoncesLaDireccionEs(DireccionOrden esperada) {
        assertThat(direccion).isEqualTo(esperada);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }
}
