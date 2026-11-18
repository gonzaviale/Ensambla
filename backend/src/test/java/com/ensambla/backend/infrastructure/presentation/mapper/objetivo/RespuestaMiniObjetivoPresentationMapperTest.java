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
    private ResponderMiniObjetivoRequest pedidoDeResponder;
    private ListarRespuestasPendientesRequest pedidoDePendientes;
    private RespuestaMiniObjetivoResponse respuestaMapeada;
    private List<RespuestaPendienteResponse> pendientes;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, los ids de la ruta y el body")
    void armaElPedidoConElUsuarioDeLaSesionLosIdsDeLaRutaYElBody() {
        cuandoSeArmaElPedidoDeResponder();

        entoncesElPedidoDeResponderTraeUsuarioIdsYBody();
    }

    @Test
    @DisplayName("Arma el pedido de pendientes con el usuario de la sesión")
    void armaElPedidoDePendientesConElUsuarioDeLaSesion() {
        cuandoSeArmaElPedidoDePendientes();

        entoncesElPedidoDePendientesTraeElUsuario();
    }

    @Test
    @DisplayName("Mapea cada campo de la respuesta")
    void mapeaCadaCampoDeLaRespuesta() {
        cuandoSeMapeaLaRespuesta();

        entoncesSeMapeaCadaCampoDeLaRespuesta();
    }

    @Test
    @DisplayName("Mapea los pendientes con el objetivo y el mini-objetivo manual")
    void mapeaLosPendientesConElObjetivoYElMiniObjetivoManual() {
        cuandoSeMapeanLosPendientes(ListarRespuestasPendientesResponse.de(List.of(objetivo()), List.of(miniManual())));

        entoncesSeMapeaElPendienteConSuObjetivoYSuManual();
    }

    @Test
    @DisplayName("Sin pendientes se mapea una lista vacía")
    void sinPendientesSeMapeaUnaListaVacia() {
        cuandoSeMapeanLosPendientes(ListarRespuestasPendientesResponse.de(List.of(), List.of()));

        entoncesNoHayPendientes();
    }

    // --- act ---
    private void cuandoSeArmaElPedidoDeResponder() {
        pedidoDeResponder = mapper.toResponderRequest(USUARIO_ID, OBJETIVO_ID, OTRO_MINI_ID,
                new ResponderMiniObjetivoWebRequest(HizoManual.NO, QuePaso.NO_FUNCIONO, COMENTARIO));
    }

    private void cuandoSeArmaElPedidoDePendientes() {
        pedidoDePendientes = mapper.toListarPendientesRequest(USUARIO_ID);
    }

    private void cuandoSeMapeaLaRespuesta() {
        respuestaMapeada = mapper.toResponse(ResponderMiniObjetivoResponse.de(respuesta()));
    }

    private void cuandoSeMapeanLosPendientes(ListarRespuestasPendientesResponse respuestas) {
        pendientes = mapper.toResponse(respuestas);
    }

    // --- assert ---
    private void entoncesElPedidoDeResponderTraeUsuarioIdsYBody() {
        assertThat(pedidoDeResponder.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeResponder.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(pedidoDeResponder.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(pedidoDeResponder.hizo()).isEqualTo(HizoManual.NO);
        assertThat(pedidoDeResponder.quePaso()).isEqualTo(QuePaso.NO_FUNCIONO);
        assertThat(pedidoDeResponder.comentario()).isEqualTo(COMENTARIO);
    }

    private void entoncesElPedidoDePendientesTraeElUsuario() {
        assertThat(pedidoDePendientes.usuarioId()).isEqualTo(USUARIO_ID);
    }

    private void entoncesSeMapeaCadaCampoDeLaRespuesta() {
        assertThat(respuestaMapeada.id()).isEqualTo(RESPUESTA_ID);
        assertThat(respuestaMapeada.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(respuestaMapeada.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(respuestaMapeada.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(respuestaMapeada.comentario()).isEqualTo(COMENTARIO);
        assertThat(respuestaMapeada.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(respuestaMapeada.respondidaEn()).isEqualTo(AHORA);
    }

    private void entoncesSeMapeaElPendienteConSuObjetivoYSuManual() {
        assertThat(pendientes).singleElement().satisfies(pendiente -> {
            assertThat(pendiente.objetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(pendiente.objetivoTexto()).isEqualTo(TEXTO);
            assertThat(pendiente.miniObjetivo().id()).isEqualTo(OTRO_MINI_ID);
            assertThat(pendiente.miniObjetivo().titulo()).isEqualTo(TITULO_MANUAL);
            assertThat(pendiente.miniObjetivo().tipo()).isNull();
        });
    }

    private void entoncesNoHayPendientes() {
        assertThat(pendientes).isEmpty();
    }
}
