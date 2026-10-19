package com.ensambla.backend.infrastructure.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorCodigoAleatorioAdapterTest {

    private final GeneradorCodigoAleatorioAdapter generadorCodigoAleatorioAdapter =
            new GeneradorCodigoAleatorioAdapter();

    @Test
    @DisplayName("Genera códigos de 6 dígitos, con ceros a la izquierda si hace falta")
    void generarShouldReturnSixDigits() {
        Set<String> codigos = generarMuchos();

        assertThat(codigos).allMatch(codigo -> codigo.matches("\\d{6}"));
    }

    @Test
    @DisplayName("No repite siempre el mismo código")
    void generarShouldVary() {
        Set<String> codigos = generarMuchos();

        assertThat(codigos).hasSizeGreaterThan(900);
    }

    // --- act ---
    private Set<String> generarMuchos() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codigos.add(generadorCodigoAleatorioAdapter.generar());
        }
        return codigos;
    }
}
