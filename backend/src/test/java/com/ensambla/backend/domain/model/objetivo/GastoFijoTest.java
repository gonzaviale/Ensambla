package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GastoFijoTest {

    private boolean esFijo;

    @ParameterizedTest(name = "{0}: {1}")
    @DisplayName("Reconoce los gastos fijos por las palabras de la categoría, escrita como venga")
    @CsvSource(delimiter = '|', textBlock = """
            Alquiler              | true
            alquileres del local  | true
            Sueldo empleada       | true
            Impuestos municipales | true
            Cuota del préstamo    | true
            Servicios             | false
            Bolsas                | false
            Cable                 | false
            """)
    void reconoceLosGastosFijosPorLasPalabrasDeLaCategoriaEscritaComoVenga(String categoria, boolean esperado) {
        cuandoSePreguntaSiEsFijo(categoria);

        entoncesLaRespuestaEs(esperado);
    }

    // --- act ---
    private void cuandoSePreguntaSiEsFijo(String categoria) {
        esFijo = GastoFijo.es(categoria);
    }

    // --- assert ---
    private void entoncesLaRespuestaEs(boolean esperado) {
        assertThat(esFijo).isEqualTo(esperado);
    }
}
