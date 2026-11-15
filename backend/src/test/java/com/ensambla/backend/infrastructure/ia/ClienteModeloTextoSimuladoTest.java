package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModeloTextoSimuladoTest {

    private final ClienteModeloTextoSimulado cliente = new ClienteModeloTextoSimulado();

    @Test
    @DisplayName("Elige la primera plantilla de la lista, sin costo")
    void completarShouldPickFirstTemplateWithoutCost() {
        PeticionModeloTexto peticion = new PeticionModeloTexto("instrucciones",
                "Plantillas disponibles:\n- Ventas: columnas Fecha (FECHA)\n- Gastos: columnas Monto (NUMERO)\n", "{}");

        RespuestaModeloTexto result = cliente.completar(peticion);

        assertThat(result.json()).isEqualTo("{\"plantilla\":\"Ventas\",\"columnas\":[],\"confianza\":0.5}");
        assertThat(result.proveedor()).isEqualTo("simulado");
        assertThat(result.tokensEntrada()).isZero();
        assertThat(result.tokensSalida()).isZero();
    }

    @Test
    @DisplayName("Sin plantillas en el contexto devuelve una plantilla vacía")
    void completarShouldReturnEmptyTemplateWhenThereAreNone() {
        RespuestaModeloTexto result = cliente.completar(new PeticionModeloTexto("i", "nada", "{}"));

        assertThat(result.json()).contains("\"plantilla\":\"\"");
    }

    @Test
    @DisplayName("En la conversación de un objetivo elige la primera métrica del catálogo y suma una acción manual")
    void completarShouldAnswerAnObjectiveConversationWithTheFirstMetric() {
        PeticionModeloTexto peticion = new PeticionModeloTexto("i", PromptConversacionObjetivos.CATALOGO
                + "\nc1: tipo SUBIR_PRECIO, sobre Yerba.\nc2: tipo MEJORAR_MARGEN, sobre el negocio.", "{}");

        RespuestaModeloTexto result = cliente.completar(peticion);

        assertThat(result.json())
                .contains("{\"modo\":\"MEDIDO\",\"codigo\":\"c1\"}", "{\"modo\":\"MANUAL\"", "\"esMonetario\":false")
                .doesNotContain("\"c2\"");
        assertThat(result.proveedor()).isEqualTo("simulado");
        assertThat(result.tokensEntrada()).isZero();
    }

    @Test
    @DisplayName("Con el catálogo vacío la conversación de un objetivo sólo propone la acción manual")
    void completarShouldAnswerAnObjectiveConversationWithoutMetrics() {
        PeticionModeloTexto peticion = new PeticionModeloTexto("i", PromptConversacionObjetivos.CATALOGO
                + "\n(vacío: no hay métricas para proponer; armá acciones manuales)", "{}");

        RespuestaModeloTexto result = cliente.completar(peticion);

        assertThat(result.json()).doesNotContain("MEDIDO").contains("{\"modo\":\"MANUAL\"");
    }
}
