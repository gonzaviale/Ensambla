package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosResponse;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CandidatoMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniObjetivosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ReemplazarMiniObjetivoWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class MiniObjetivoPresentationMapperTest {

    private final MiniObjetivoPresentationMapper mapper = new MiniObjetivoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listar con el usuario de la sesión y el objetivo de la ruta")
    void toListarRequestShouldCarryUserAndObjetivo() {
        ListarMiniObjetivosRequest result = mapper.toListarRequest(USUARIO_ID, OBJETIVO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Arma el pedido de candidatos con el usuario, el objetivo y el mini-objetivo")
    void toCandidatosRequestShouldCarryTheIds() {
        ListarCandidatosRequest result = mapper.toCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
    }

    @Test
    @DisplayName("Arma el pedido de reemplazo con los ids de la ruta y el candidato elegido del cuerpo")
    void toReemplazarRequestShouldCarryIdsAndTheChosenCandidate() {
        ReemplazarMiniObjetivoRequest result = mapper.toReemplazarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID,
                new ReemplazarMiniObjetivoWebRequest(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.referencia()).isEqualTo(PRODUCTO);
    }

    @Test
    @DisplayName("Arma el pedido de abandonar con los ids de la sesión y la ruta")
    void toAbandonarRequestShouldCarryTheIds() {
        AbandonarMiniObjetivoRequest result = mapper.toAbandonarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
    }

    @Test
    @DisplayName("Mapea cada mini-objetivo a su respuesta con todos sus datos")
    void toResponseShouldMapEveryFieldOfAMiniObjetivo() {
        MiniObjetivoResponse result = mapper.toResponse(MiniObjetivoItem.de(miniObjetivo()));

        assertThat(result.id()).isEqualTo(MINI_ID);
        assertThat(result.posicion()).isEqualTo(1);
        assertThat(result.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
        assertThat(result.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(result.titulo()).isEqualTo(TITULO);
        assertThat(result.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.referencia()).isEqualTo(PRODUCTO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.valorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(result.meta()).isEqualTo(META_MINI);
        assertThat(result.avance()).isEqualByComparingTo("0");
        assertThat(result.aporteEstimado()).isEqualTo(APORTE);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
        assertThat(result.textoConIa()).isFalse();
    }

    @Test
    @DisplayName("Mapea la lista de mini-objetivos con su historia y lo que falta")
    void toResponseShouldMapTheListWithHistoryAndMissingData() {
        ListarMiniObjetivosResponse respuesta = new ListarMiniObjetivosResponse(
                List.of(MiniObjetivoItem.de(miniObjetivo())),
                List.of(MiniObjetivoItem.de(miniObjetivo()), MiniObjetivoItem.de(miniObjetivo())),
                FaltanDatosMiniObjetivos.POCOS_DATOS);

        MiniObjetivosResponse result = mapper.toResponse(respuesta);

        assertThat(result.minis()).hasSize(1);
        assertThat(result.historial()).hasSize(2);
        assertThat(result.faltanDatos()).isEqualTo(FaltanDatosMiniObjetivos.POCOS_DATOS);
    }

    @Test
    @DisplayName("Mapea las alternativas con todos sus datos")
    void toResponseShouldMapEveryFieldOfTheCandidates() {
        List<CandidatoMiniObjetivoResponse> result =
                mapper.toResponse(ListarCandidatosResponse.de(List.of(candidato())));

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
            assertThat(item.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
            assertThat(item.titulo()).isEqualTo(TITULO);
            assertThat(item.justificacion()).isEqualTo(JUSTIFICACION);
            assertThat(item.referencia()).isEqualTo(PRODUCTO);
            assertThat(item.registroId()).isEqualTo(PRODUCTO_ID);
            assertThat(item.valorInicial()).isEqualTo(VALOR_INICIAL);
            assertThat(item.meta()).isEqualTo(META_MINI);
            assertThat(item.aporteEstimado()).isEqualTo(APORTE);
        });
    }

    @Test
    @DisplayName("Sin alternativas devuelve una lista vacía")
    void toResponseShouldReturnAnEmptyListWithoutCandidates() {
        assertThat(mapper.toResponse(ListarCandidatosResponse.de(List.of()))).isEmpty();
    }
}
