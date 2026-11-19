package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class PedidoRenovacionTest {

    private MiniObjetivo mini;
    private RespuestaMiniObjetivo respuesta;
    private PedidoRenovacion pedido;

    @Test
    @DisplayName("Lleva la acción anterior, qué hacer con ella y lo que respondió la persona")
    void llevaLaAccionAnteriorQueHacerConEllaYLoQueRespondioLaPersona() {
        dadoUnaRespuesta(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, "Hablé con 1 sola vecina");

        cuandoSeArmaElPedido();

        entoncesElPedidoLlevaLaAccionAnteriorYLaRespuesta();
    }

    @Test
    @DisplayName("A la IA no le llegan correos ni teléfonos del comentario")
    void aLaIaNoLeLleganCorreosNiTelefonosDelComentario() {
        dadoUnaRespuesta(HizoManual.SI, QuePaso.FUNCIONO, "Le escribí a ana@mail.com y al 11 5555 5555");

        cuandoSeArmaElPedido();

        entoncesElComentarioEs("Le escribí a [dato oculto] y al [dato oculto]");
    }

    @Test
    @DisplayName("Sin comentario el pedido queda sin comentario")
    void sinComentarioElPedidoQuedaSinComentario() {
        dadoUnaRespuesta(HizoManual.NO, QuePaso.OTRA, null);

        cuandoSeArmaElPedido();

        entoncesNoHayComentario();
    }

    // --- arrange ---
    private void dadoUnaRespuesta(HizoManual hizo, QuePaso quePaso, String comentario) {
        mini = miniManual();
        respuesta = RespuestaMiniObjetivo.de(mini, hizo, quePaso, comentario, AHORA);
    }

    // --- act ---
    private void cuandoSeArmaElPedido() {
        pedido = PedidoRenovacion.de(mini, respuesta);
    }

    // --- assert ---
    private void entoncesElPedidoLlevaLaAccionAnteriorYLaRespuesta() {
        assertThat(pedido.accion()).isEqualTo(AccionRenovacion.SE_DIVIDE);
        assertThat(pedido.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(pedido.justificacion()).isEqualTo(JUSTIFICACION_MANUAL);
        assertThat(pedido.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(pedido.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(pedido.comentario()).isEqualTo("Hablé con 1 sola vecina");
    }

    private void entoncesElComentarioEs(String esperado) {
        assertThat(pedido.comentario()).isEqualTo(esperado);
    }

    private void entoncesNoHayComentario() {
        assertThat(pedido.comentario()).isNull();
    }
}
