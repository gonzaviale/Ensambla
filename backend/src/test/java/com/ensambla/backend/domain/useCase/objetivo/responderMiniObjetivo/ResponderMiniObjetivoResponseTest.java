package com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.COMENTARIO;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.RESPUESTA_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.SEMANA_RESPUESTA;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;

class ResponderMiniObjetivoResponseTest {

    @Test
    @DisplayName("Copia todos los datos de la respuesta")
    void deShouldCopyEveryField() {
        ResponderMiniObjetivoResponse result = ResponderMiniObjetivoResponse.de(respuesta());

        assertThat(result.id()).isEqualTo(RESPUESTA_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.comentario()).isEqualTo(COMENTARIO);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(result.respondidaEn()).isEqualTo(AHORA);
    }
}
