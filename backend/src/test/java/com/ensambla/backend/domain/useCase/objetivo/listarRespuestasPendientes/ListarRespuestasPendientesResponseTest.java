package com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO_SIN_META;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;

class ListarRespuestasPendientesResponseTest {

    private static final ObjetivoId OTRO_OBJETIVO_ID = ObjetivoId.de("00000000-0000-4000-8000-0000000000b1");

    @Test
    @DisplayName("Cada pendiente trae el texto del objetivo al que pertenece, en el orden recibido")
    void deShouldPairEachMiniObjetivoWithItsObjetivoInOrder() {
        Objetivo otro = objetivoSinMeta();
        otro.setId(OTRO_OBJETIVO_ID);
        MiniObjetivo delOtro = miniManual();
        delOtro.setObjetivoId(OTRO_OBJETIVO_ID);

        ListarRespuestasPendientesResponse result = ListarRespuestasPendientesResponse.de(
                List.of(objetivo(), otro), List.of(delOtro, miniManual()));

        assertThat(result.pendientes()).extracting(ListarRespuestasPendientesResponse.Pendiente::objetivoTexto)
                .containsExactly(TEXTO_SIN_META, TEXTO);
        assertThat(result.pendientes()).extracting(ListarRespuestasPendientesResponse.Pendiente::objetivoId)
                .containsExactly(OTRO_OBJETIVO_ID, OBJETIVO_ID);
        assertThat(result.pendientes().getFirst().miniObjetivo().id()).isEqualTo(OTRO_MINI_ID);
    }
}
