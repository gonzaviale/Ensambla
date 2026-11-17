package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;

class ManualCierreTest {

    private ManualCierre cierre;

    @Test
    @DisplayName("Un manual respondido trae lo que contó la persona")
    void respondidoShouldCarryTheAnswer() {
        whenSeCierraUnManualRespondido();

        thenElCierreTraeLoQueContoLaPersona();
    }

    @Test
    @DisplayName("Un manual sin respuesta no trae qué hizo ni qué pasó")
    void sinRespuestaShouldHaveNoAnswer() {
        whenSeCierraUnManualSinRespuesta();

        thenElCierreNoTraeRespuesta();
    }

    // --- act ---
    private void whenSeCierraUnManualRespondido() {
        cierre = ManualCierre.respondido(miniManual(), respuesta());
    }

    private void whenSeCierraUnManualSinRespuesta() {
        cierre = ManualCierre.sinRespuesta(miniManual());
    }

    // --- assert ---
    private void thenElCierreTraeLoQueContoLaPersona() {
        assertThat(cierre.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(cierre.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(cierre.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(cierre.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(cierre.respondido()).isTrue();
    }

    private void thenElCierreNoTraeRespuesta() {
        assertThat(cierre.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(cierre.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(cierre.hizo()).isNull();
        assertThat(cierre.quePaso()).isNull();
        assertThat(cierre.respondido()).isFalse();
    }
}
