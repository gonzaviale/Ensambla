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

    private ResponderMiniObjetivoResponse resultado;

    @Test
    @DisplayName("Copia todos los datos de la respuesta")
    void deShouldCopyEveryField() {
        whenSeArmaLaRespuesta();

        thenSeCopianTodosLosDatos();
    }

    // --- act ---
    private void whenSeArmaLaRespuesta() {
        resultado = ResponderMiniObjetivoResponse.de(respuesta());
    }

    // --- assert ---
    private void thenSeCopianTodosLosDatos() {
        assertThat(resultado.id()).isEqualTo(RESPUESTA_ID);
        assertThat(resultado.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(resultado.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(resultado.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(resultado.comentario()).isEqualTo(COMENTARIO);
        assertThat(resultado.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(resultado.respondidaEn()).isEqualTo(AHORA);
    }
}
