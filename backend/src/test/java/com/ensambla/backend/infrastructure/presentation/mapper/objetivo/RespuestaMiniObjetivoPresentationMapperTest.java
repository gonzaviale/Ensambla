package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesResponse;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.RespuestaMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.RespuestaPendienteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ResponderMiniObjetivoWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.COMENTARIO;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.RESPUESTA_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.SEMANA_RESPUESTA;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;

class RespuestaMiniObjetivoPresentationMapperTest {

    private final RespuestaMiniObjetivoPresentationMapper mapper =
            new RespuestaMiniObjetivoPresentationMapper(new MiniObjetivoPresentationMapper());

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, los ids de la ruta y el body")
    void toResponderRequestShouldCarryUsuarioIdsAndBody() {
        ResponderMiniObjetivoRequest result = mapper.toResponderRequest(USUARIO_ID, OBJETIVO_ID, OTRO_MINI_ID,
                new ResponderMiniObjetivoWebRequest(HizoManual.NO, QuePaso.NO_FUNCIONO, COMENTARIO));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.hizo()).isEqualTo(HizoManual.NO);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_FUNCIONO);
        assertThat(result.comentario()).isEqualTo(COMENTARIO);
    }

    @Test
    @DisplayName("Arma el pedido de pendientes con el usuario de la sesión")
    void toListarPendientesRequestShouldCarrySessionUsuario() {
        ListarRespuestasPendientesRequest result = mapper.toListarPendientesRequest(USUARIO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Mapea cada campo de la respuesta")
    void toResponseShouldMapEveryFieldOfTheAnswer() {
        RespuestaMiniObjetivoResponse result = mapper.toResponse(ResponderMiniObjetivoResponse.de(respuesta()));

        assertThat(result.id()).isEqualTo(RESPUESTA_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.comentario()).isEqualTo(COMENTARIO);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(result.respondidaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea los pendientes con el objetivo y el mini-objetivo manual")
    void toResponseShouldMapThePendingOnes() {
        ListarRespuestasPendientesResponse pendientes =
                ListarRespuestasPendientesResponse.de(List.of(objetivo()), List.of(miniManual()));

        List<RespuestaPendienteResponse> result = mapper.toResponse(pendientes);

        assertThat(result).singleElement().satisfies(pendiente -> {
            assertThat(pendiente.objetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(pendiente.objetivoTexto()).isEqualTo(TEXTO);
            assertThat(pendiente.miniObjetivo().id()).isEqualTo(OTRO_MINI_ID);
            assertThat(pendiente.miniObjetivo().titulo()).isEqualTo(TITULO_MANUAL);
            assertThat(pendiente.miniObjetivo().tipo()).isNull();
        });
    }

    @Test
    @DisplayName("Sin pendientes se mapea una lista vacía")
    void toResponseShouldMapAnEmptyList() {
        assertThat(mapper.toResponse(ListarRespuestasPendientesResponse.de(List.of(), List.of()))).isEmpty();
    }
}
