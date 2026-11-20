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
    void unaAccionValidaQuedaConElTextoSinEspaciosEnLosExtremos() {
        cuandoSeArmaLaAccion("  " + TITULO + " ", JUSTIFICACION + "\n");

        entoncesLaAccionEsLaValida();
    }

    @Test
    @DisplayName("Un título de 80 caracteres y una justificación de 280 se aceptan")
    void unTituloDe80CaracteresYUnaJustificacionDe280SeAceptan() {
        cuandoSeArmaLaAccion("a".repeat(LARGO_MAXIMO_DE_TITULO), "b".repeat(LARGO_MAXIMO_DE_JUSTIFICACION));

        entoncesHayAccion();
    }

    @Test
    @DisplayName("Sin título no hay acción")
    void sinTituloNoHayAccion() {
        cuandoSeArmaLaAccion(null, JUSTIFICACION);

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Con un título en blanco no hay acción")
    void conUnTituloEnBlancoNoHayAccion() {
        cuandoSeArmaLaAccion("   ", JUSTIFICACION);

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Con un título de más de 80 caracteres no hay acción")
    void conUnTituloDeMasDe80CaracteresNoHayAccion() {
        cuandoSeArmaLaAccion("a".repeat(LARGO_MAXIMO_DE_TITULO + 1), JUSTIFICACION);

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Sin justificación no hay acción")
    void sinJustificacionNoHayAccion() {
        cuandoSeArmaLaAccion(TITULO, null);

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Con una justificación en blanco no hay acción")
    void conUnaJustificacionEnBlancoNoHayAccion() {
        cuandoSeArmaLaAccion(TITULO, " ");

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Con una justificación de más de 280 caracteres no hay acción")
    void conUnaJustificacionDeMasDe280CaracteresNoHayAccion() {
        cuandoSeArmaLaAccion(TITULO, "b".repeat(LARGO_MAXIMO_DE_JUSTIFICACION + 1));

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Un título con un importe en pesos no es una acción")
    void unTituloConUnImporteEnPesosNoEsUnaAccion() {
        cuandoSeArmaLaAccion("Cobrá $5.000 de más", JUSTIFICACION);

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Una justificación con un porcentaje con espacio no es una acción")
    void unaJustificacionConUnPorcentajeConEspacioNoEsUnaAccion() {
        cuandoSeArmaLaAccion(TITULO, "Vas a vender un 20 % más");

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("Una justificación con un porcentaje pegado no es una acción")
    void unaJustificacionConUnPorcentajePegadoNoEsUnaAccion() {
        cuandoSeArmaLaAccion(TITULO, "Vas a vender 20% más");

        entoncesNoHayAccion();
    }

    @Test
    @DisplayName("El seguimiento de respaldo pide contar cada semana cómo va el objetivo, con sus palabras")
    void elSeguimientoDeRespaldoPideContarCadaSemanaComoVaElObjetivoConSusPalabras() {
        cuandoSeArmaElSeguimiento("  Quiero vender más  ");

        entoncesElSeguimientoPideContarCadaSemana();
    }

    @Test
    @DisplayName("El seguimiento acorta un objetivo de más de 200 caracteres")
    void elSeguimientoAcortaUnObjetivoDeMasDe200Caracteres() {
        cuandoSeArmaElSeguimiento("a".repeat(LARGO_MAXIMO_DE_OBJETIVO + 100));

        entoncesElSeguimientoCortaElObjetivo();
    }

    // --- act ---
    private void cuandoSeArmaLaAccion(String titulo, String justificacion) {
        accion = AccionManual.de(titulo, justificacion);
    }

    private void cuandoSeArmaElSeguimiento(String objetivo) {
        seguimiento = AccionManual.seguimiento(objetivo);
    }

    // --- assert ---
    private void entoncesLaAccionEsLaValida() {
        assertThat(accion).contains(new AccionManual(TITULO, JUSTIFICACION));
    }

    private void entoncesHayAccion() {
        assertThat(accion).isPresent();
    }

    private void entoncesNoHayAccion() {
        assertThat(accion).isEmpty();
    }

    private void entoncesElSeguimientoPideContarCadaSemana() {
        assertThat(seguimiento.titulo()).isEqualTo("Cargá cómo te fue con tu objetivo");
        assertThat(seguimiento.justificacion())
                .isEqualTo("Una vez por semana contale a Sami cómo avanzaste con: Quiero vender más");
    }

    private void entoncesElSeguimientoCortaElObjetivo() {
        assertThat(seguimiento.justificacion()).endsWith("a".repeat(LARGO_MAXIMO_DE_OBJETIVO) + "…");
        assertThat(seguimiento.justificacion().length()).isLessThanOrEqualTo(AccionManual.LARGO_MAXIMO_JUSTIFICACION);
    }
}
