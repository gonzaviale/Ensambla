package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;

class ManualCierreTest {

    @Test
    @DisplayName("Un manual respondido trae lo que contó la persona")
    void respondidoShouldCarryTheAnswer() {
        ManualCierre result = ManualCierre.respondido(miniManual(), respuesta());

        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.respondido()).isTrue();
    }

    @Test
    @DisplayName("Un manual sin respuesta no trae qué hizo ni qué pasó")
    void sinRespuestaShouldHaveNoAnswer() {
        ManualCierre result = ManualCierre.sinRespuesta(miniManual());

        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(result.hizo()).isNull();
        assertThat(result.quePaso()).isNull();
        assertThat(result.respondido()).isFalse();
    }
}
