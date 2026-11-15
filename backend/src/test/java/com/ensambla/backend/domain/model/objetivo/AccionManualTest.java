package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccionManualTest {

    private static final String TITULO = "Pedile a 3 clientes que te recomienden";
    private static final String JUSTIFICACION = "Es la forma más barata de que te conozcan.";

    @Test
    @DisplayName("Una acción válida queda con el texto sin espacios en los extremos")
    void deShouldAcceptAValidAction() {
        Optional<AccionManual> result = AccionManual.de("  " + TITULO + " ", JUSTIFICACION + "\n");

        assertThat(result).contains(new AccionManual(TITULO, JUSTIFICACION));
    }

    @Test
    @DisplayName("Un título de 80 caracteres y una justificación de 280 se aceptan")
    void deShouldAcceptTheMaximumLengths() {
        assertThat(AccionManual.de("a".repeat(80), "b".repeat(280))).isPresent();
    }

    @Test
    @DisplayName("Sin título, o con un título en blanco o de más de 80 caracteres, no hay acción")
    void deShouldRejectABadTitle() {
        assertThat(AccionManual.de(null, JUSTIFICACION)).isEmpty();
        assertThat(AccionManual.de("   ", JUSTIFICACION)).isEmpty();
        assertThat(AccionManual.de("a".repeat(81), JUSTIFICACION)).isEmpty();
    }

    @Test
    @DisplayName("Sin justificación, o con una de más de 280 caracteres, no hay acción")
    void deShouldRejectABadJustification() {
        assertThat(AccionManual.de(TITULO, null)).isEmpty();
        assertThat(AccionManual.de(TITULO, " ")).isEmpty();
        assertThat(AccionManual.de(TITULO, "b".repeat(281))).isEmpty();
    }

    @Test
    @DisplayName("Una acción no puede afirmar un importe en pesos ni un porcentaje")
    void deShouldRejectAmountsAndPercentages() {
        assertThat(AccionManual.de("Cobrá $5.000 de más", JUSTIFICACION)).isEmpty();
        assertThat(AccionManual.de(TITULO, "Vas a vender un 20 % más")).isEmpty();
        assertThat(AccionManual.de(TITULO, "Vas a vender 20% más")).isEmpty();
    }

    @Test
    @DisplayName("El seguimiento de respaldo pide contar cada semana cómo va el objetivo, con sus palabras")
    void seguimientoShouldAskForAWeeklyReport() {
        AccionManual result = AccionManual.seguimiento("  Quiero vender más  ");

        assertThat(result.titulo()).isEqualTo("Anotá cómo te fue con tu objetivo");
        assertThat(result.justificacion())
                .isEqualTo("Una vez por semana contale a Sami cómo avanzaste con: Quiero vender más");
    }

    @Test
    @DisplayName("El seguimiento acorta un objetivo de más de 200 caracteres")
    void seguimientoShouldCutALongObjective() {
        AccionManual result = AccionManual.seguimiento("a".repeat(300));

        assertThat(result.justificacion()).endsWith("a".repeat(200) + "…");
        assertThat(result.justificacion().length()).isLessThanOrEqualTo(AccionManual.LARGO_MAXIMO_JUSTIFICACION);
    }
}
