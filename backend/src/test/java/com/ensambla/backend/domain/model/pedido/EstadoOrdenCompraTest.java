package com.ensambla.backend.domain.model.pedido;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra.ACEPTADA;
import static com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra.EN_PREPARACION;
import static com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra.LISTA;
import static com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra.PENDIENTE;
import static com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra.RECHAZADA;
import static org.assertj.core.api.Assertions.assertThat;

class EstadoOrdenCompraTest {

    @ParameterizedTest
    @CsvSource({"PENDIENTE,ACEPTADA", "PENDIENTE,RECHAZADA", "ACEPTADA,EN_PREPARACION", "ACEPTADA,LISTA",
            "EN_PREPARACION,LISTA"})
    @DisplayName("Son los pasos que el dueño puede dar: aceptar o rechazar, preparar y dejar lista")
    void puedePasarAShouldAllowTheNextSteps(EstadoOrdenCompra desde, EstadoOrdenCompra hasta) {
        assertThat(desde.puedePasarA(hasta)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"PENDIENTE,PENDIENTE", "PENDIENTE,EN_PREPARACION", "PENDIENTE,LISTA",
            "ACEPTADA,PENDIENTE", "ACEPTADA,ACEPTADA", "ACEPTADA,RECHAZADA",
            "EN_PREPARACION,PENDIENTE", "EN_PREPARACION,ACEPTADA", "EN_PREPARACION,RECHAZADA",
            "EN_PREPARACION,EN_PREPARACION", "RECHAZADA,PENDIENTE", "RECHAZADA,ACEPTADA", "RECHAZADA,RECHAZADA",
            "RECHAZADA,EN_PREPARACION", "RECHAZADA,LISTA", "LISTA,PENDIENTE", "LISTA,ACEPTADA", "LISTA,RECHAZADA",
            "LISTA,EN_PREPARACION", "LISTA,LISTA"})
    @DisplayName("Cualquier otro paso no se puede dar: no se vuelve atrás, no se salta y rechazada y lista son finales")
    void puedePasarAShouldRejectEveryOtherStep(EstadoOrdenCompra desde, EstadoOrdenCompra hasta) {
        assertThat(desde.puedePasarA(hasta)).isFalse();
    }

    @Test
    @DisplayName("Cada estado se dice como lo diría el dueño")
    void etiquetaShouldReadNaturally() {
        assertThat(PENDIENTE.etiqueta()).isEqualTo("pendiente");
        assertThat(ACEPTADA.etiqueta()).isEqualTo("aceptada");
        assertThat(RECHAZADA.etiqueta()).isEqualTo("rechazada");
        assertThat(EN_PREPARACION.etiqueta()).isEqualTo("en preparación");
        assertThat(LISTA.etiqueta()).isEqualTo("lista para retirar");
    }
}
