package com.ensambla.backend.domain.model.ubicacion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ConsultaDireccionTest {

    private ConsultaDireccion consulta;
    private Throwable rechazo;

    @Test
    @DisplayName("Normaliza espacios y conserva la localidad")
    void normalizaEspaciosYConservaLaLocalidad() {
        cuandoSeEscribeLaDireccion("  Florencio   Varela 1903, San Justo  ");

        entoncesElTextoQuedaNormalizado();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "    ", "abcd"})
    @DisplayName("Rechaza una dirección demasiado corta")
    void rechazaUnaDireccionDemasiadoCorta(String escrito) {
        cuandoSeIntentaEscribirLaDireccion(escrito);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza una dirección de más de 200 caracteres")
    void rechazaUnaDireccionDeMasDe200Caracteres() {
        cuandoSeIntentaEscribirLaDireccion("a".repeat(201));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 200})
    @DisplayName("Acepta los largos límite")
    void aceptaLosLargosLimite(int largo) {
        cuandoSeEscribeLaDireccion("a".repeat(largo));

        entoncesElTextoTieneElLargo(largo);
    }

    // --- cuando ---

    private void cuandoSeEscribeLaDireccion(String escrito) {
        consulta = ConsultaDireccion.de(escrito);
    }

    private void cuandoSeIntentaEscribirLaDireccion(String escrito) {
        rechazo = catchThrowable(() -> ConsultaDireccion.de(escrito));
    }

    // --- entonces ---

    private void entoncesElTextoQuedaNormalizado() {
        assertThat(consulta.texto()).isEqualTo("Florencio Varela 1903, San Justo");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesElTextoTieneElLargo(int largo) {
        assertThat(consulta.texto()).hasSize(largo);
    }
}
