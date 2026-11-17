package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModeloTextoSimuladoTest {

    private final ClienteModeloTextoSimulado cliente = new ClienteModeloTextoSimulado();
    private RespuestaModeloTexto respuesta;

    @Test
    @DisplayName("Elige la primera plantilla de la lista, sin costo")
    void completarShouldPickFirstTemplateWithoutCost() {
        whenSeCompleta("Plantillas disponibles:\n- Ventas: columnas Fecha (FECHA)\n- Gastos: columnas Monto (NUMERO)\n");

        thenSeEligeLaPrimeraPlantillaSinCosto();
    }

    @Test
    @DisplayName("Sin plantillas en el contexto devuelve una plantilla vacía")
    void completarShouldReturnEmptyTemplateWhenThereAreNone() {
        whenSeCompleta("nada");

        thenLaPlantillaEsVacia();
    }

    @Test
    @DisplayName("En la conversación de un objetivo elige la primera métrica del catálogo y suma una acción manual")
    void completarShouldAnswerAnObjectiveConversationWithTheFirstMetric() {
        whenSeCompleta(PromptConversacionObjetivos.CATALOGO
                + "\nc1: tipo SUBIR_PRECIO, sobre Yerba.\nc2: tipo MEJORAR_MARGEN, sobre el negocio.");

        thenSeEligeLaPrimeraMetricaYSeSumaUnaAccionManual();
    }

    @Test
    @DisplayName("Con el catálogo vacío la conversación de un objetivo sólo propone la acción manual")
    void completarShouldAnswerAnObjectiveConversationWithoutMetrics() {
        whenSeCompleta(PromptConversacionObjetivos.CATALOGO
                + "\n(vacío: no hay métricas para proponer; armá acciones manuales)");

        thenSoloSeProponeLaAccionManual();
    }

    @Test
    @DisplayName("En las renovaciones de manuales redacta un texto genérico por cada código del pedido")
    void completarShouldAnswerEveryRenewalWithAGenericText() {
        whenSeCompleta(PromptRenovacionManuales.RENOVACIONES
                + "\nr1: qué hacer: SE_DIVIDE.\nr2: qué hacer: CAMBIA.");

        thenSeRedactaUnTextoGenericoPorCadaCodigo();
    }

    // --- act ---
    private void whenSeCompleta(String contexto) {
        respuesta = cliente.completar(new PeticionModeloTexto("i", contexto, "{}"));
    }

    // --- assert ---
    private void thenSeEligeLaPrimeraPlantillaSinCosto() {
        assertThat(respuesta.json()).isEqualTo("{\"plantilla\":\"Ventas\",\"columnas\":[],\"confianza\":0.5}");
        assertThat(respuesta.proveedor()).isEqualTo("simulado");
        assertThat(respuesta.tokensEntrada()).isZero();
        assertThat(respuesta.tokensSalida()).isZero();
    }

    private void thenLaPlantillaEsVacia() {
        assertThat(respuesta.json()).contains("\"plantilla\":\"\"");
    }

    private void thenSeEligeLaPrimeraMetricaYSeSumaUnaAccionManual() {
        assertThat(respuesta.json())
                .contains("{\"modo\":\"MEDIDO\",\"codigo\":\"c1\"}", "{\"modo\":\"MANUAL\"", "\"esMonetario\":false")
                .doesNotContain("\"c2\"");
        assertThat(respuesta.proveedor()).isEqualTo("simulado");
        assertThat(respuesta.tokensEntrada()).isZero();
    }

    private void thenSoloSeProponeLaAccionManual() {
        assertThat(respuesta.json()).doesNotContain("MEDIDO").contains("{\"modo\":\"MANUAL\"");
    }

    private void thenSeRedactaUnTextoGenericoPorCadaCodigo() {
        assertThat(respuesta.json())
                .contains("\"codigo\":\"r1\"", "\"codigo\":\"r2\"", "Seguí con tu objetivo esta semana");
        assertThat(respuesta.proveedor()).isEqualTo("simulado");
        assertThat(respuesta.tokensSalida()).isZero();
    }
}
