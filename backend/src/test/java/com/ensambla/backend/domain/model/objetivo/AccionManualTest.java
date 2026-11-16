package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccionManualTest {

    private static final String TITULO = "Pedile a 3 clientes que te recomienden";
    private static final String JUSTIFICACION = "Es la forma más barata de que te conozcan.";
    private static final int LARGO_MAXIMO_DE_TITULO = 80;
    private static final int LARGO_MAXIMO_DE_JUSTIFICACION = 280;
    private static final int LARGO_MAXIMO_DE_OBJETIVO = 200;

    private Optional<AccionManual> accion;
    private AccionManual seguimiento;

    @Test
    @DisplayName("Una acción válida queda con el texto sin espacios en los extremos")
    void deShouldAcceptAValidAction() {
        whenSeArmaLaAccion("  " + TITULO + " ", JUSTIFICACION + "\n");

        thenLaAccionEsLaValida();
    }

    @Test
    @DisplayName("Un título de 80 caracteres y una justificación de 280 se aceptan")
    void deShouldAcceptTheMaximumLengths() {
        whenSeArmaLaAccion("a".repeat(LARGO_MAXIMO_DE_TITULO), "b".repeat(LARGO_MAXIMO_DE_JUSTIFICACION));

        thenHayAccion();
    }

    @Test
    @DisplayName("Sin título no hay acción")
    void deShouldRejectAMissingTitle() {
        whenSeArmaLaAccion(null, JUSTIFICACION);

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Con un título en blanco no hay acción")
    void deShouldRejectABlankTitle() {
        whenSeArmaLaAccion("   ", JUSTIFICACION);

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Con un título de más de 80 caracteres no hay acción")
    void deShouldRejectATitleThatIsTooLong() {
        whenSeArmaLaAccion("a".repeat(LARGO_MAXIMO_DE_TITULO + 1), JUSTIFICACION);

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Sin justificación no hay acción")
    void deShouldRejectAMissingJustification() {
        whenSeArmaLaAccion(TITULO, null);

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Con una justificación en blanco no hay acción")
    void deShouldRejectABlankJustification() {
        whenSeArmaLaAccion(TITULO, " ");

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Con una justificación de más de 280 caracteres no hay acción")
    void deShouldRejectAJustificationThatIsTooLong() {
        whenSeArmaLaAccion(TITULO, "b".repeat(LARGO_MAXIMO_DE_JUSTIFICACION + 1));

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Un título con un importe en pesos no es una acción")
    void deShouldRejectAnAmountInTheTitle() {
        whenSeArmaLaAccion("Cobrá $5.000 de más", JUSTIFICACION);

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Una justificación con un porcentaje con espacio no es una acción")
    void deShouldRejectASpacedPercentage() {
        whenSeArmaLaAccion(TITULO, "Vas a vender un 20 % más");

        thenNoHayAccion();
    }

    @Test
    @DisplayName("Una justificación con un porcentaje pegado no es una acción")
    void deShouldRejectAGluedPercentage() {
        whenSeArmaLaAccion(TITULO, "Vas a vender 20% más");

        thenNoHayAccion();
    }

    @Test
    @DisplayName("El seguimiento de respaldo pide contar cada semana cómo va el objetivo, con sus palabras")
    void seguimientoShouldAskForAWeeklyReport() {
        whenSeArmaElSeguimiento("  Quiero vender más  ");

        thenElSeguimientoPideContarCadaSemana();
    }

    @Test
    @DisplayName("El seguimiento acorta un objetivo de más de 200 caracteres")
    void seguimientoShouldCutALongObjective() {
        whenSeArmaElSeguimiento("a".repeat(LARGO_MAXIMO_DE_OBJETIVO + 100));

        thenElSeguimientoCortaElObjetivo();
    }

    // --- act ---
    private void whenSeArmaLaAccion(String titulo, String justificacion) {
        accion = AccionManual.de(titulo, justificacion);
    }

    private void whenSeArmaElSeguimiento(String objetivo) {
        seguimiento = AccionManual.seguimiento(objetivo);
    }

    // --- assert ---
    private void thenLaAccionEsLaValida() {
        assertThat(accion).contains(new AccionManual(TITULO, JUSTIFICACION));
    }

    private void thenHayAccion() {
        assertThat(accion).isPresent();
    }

    private void thenNoHayAccion() {
        assertThat(accion).isEmpty();
    }

    private void thenElSeguimientoPideContarCadaSemana() {
        assertThat(seguimiento.titulo()).isEqualTo("Anotá cómo te fue con tu objetivo");
        assertThat(seguimiento.justificacion())
                .isEqualTo("Una vez por semana contale a Sami cómo avanzaste con: Quiero vender más");
    }

    private void thenElSeguimientoCortaElObjetivo() {
        assertThat(seguimiento.justificacion()).endsWith("a".repeat(LARGO_MAXIMO_DE_OBJETIVO) + "…");
        assertThat(seguimiento.justificacion().length()).isLessThanOrEqualTo(AccionManual.LARGO_MAXIMO_JUSTIFICACION);
    }
}
