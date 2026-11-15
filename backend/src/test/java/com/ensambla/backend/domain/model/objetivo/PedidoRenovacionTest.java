package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class PedidoRenovacionTest {

    @Test
    @DisplayName("Lleva la acción anterior, qué hacer con ella y lo que respondió la persona")
    void deShouldCarryThePreviousActionAndTheAnswer() {
        MiniObjetivo mini = miniManual();
        RespuestaMiniObjetivo respuesta = RespuestaMiniObjetivo.de(
                mini, HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, "Hablé con 1 sola vecina", AHORA);

        PedidoRenovacion result = PedidoRenovacion.de(mini, respuesta);

        assertThat(result.accion()).isEqualTo(AccionRenovacion.SE_DIVIDE);
        assertThat(result.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(result.justificacion()).isEqualTo(JUSTIFICACION_MANUAL);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.comentario()).isEqualTo("Hablé con 1 sola vecina");
    }

    @Test
    @DisplayName("A la IA no le llegan correos ni teléfonos del comentario")
    void deShouldHidePersonalDataOfTheComment() {
        MiniObjetivo mini = miniManual();
        RespuestaMiniObjetivo respuesta = RespuestaMiniObjetivo.de(
                mini, HizoManual.SI, QuePaso.FUNCIONO, "Le escribí a ana@mail.com y al 11 5555 5555", AHORA);

        assertThat(PedidoRenovacion.de(mini, respuesta).comentario())
                .isEqualTo("Le escribí a [dato oculto] y al [dato oculto]");
    }

    @Test
    @DisplayName("Sin comentario el pedido queda sin comentario")
    void deShouldKeepAMissingComment() {
        MiniObjetivo mini = miniManual();
        RespuestaMiniObjetivo respuesta = RespuestaMiniObjetivo.de(mini, HizoManual.NO, QuePaso.OTRA, null, AHORA);

        assertThat(PedidoRenovacion.de(mini, respuesta).comentario()).isNull();
    }
}
