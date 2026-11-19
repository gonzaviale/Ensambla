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
    void armaElPedidoDeListar() {
        entoncesElPedidoEs(mapper.toListarRequest(USUARIO_ID, OBJETIVO_ID), new ListarMiniObjetivosRequest(USUARIO_ID, OBJETIVO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de candidatos con el usuario, el objetivo y el mini-objetivo")
    void armaElPedidoDeCandidatos() {
        entoncesElPedidoEs(mapper.toCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID),
                new ListarCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    @Test
    @DisplayName("Arma el pedido de reemplazo con los ids de la ruta y el candidato elegido del cuerpo")
    void armaElPedidoDeReemplazo() {
        entoncesElPedidoEs(mapper.toReemplazarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID,
                        new ReemplazarMiniObjetivoWebRequest(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO)),
                new ReemplazarMiniObjetivoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID, TipoMiniObjetivo.SUBIR_PRECIO,
                        PRODUCTO_ID, PRODUCTO));
    }

    @Test
    @DisplayName("Arma el pedido de abandonar con los ids de la sesión y la ruta")
    void armaElPedidoDeAbandonar() {
        entoncesElPedidoEs(mapper.toAbandonarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID),
                new AbandonarMiniObjetivoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    @Test
    @DisplayName("Mapea cada mini-objetivo a su respuesta con todos sus datos")
    void mapeaCadaMiniObjetivoConTodosSusDatos() {
        entoncesElMiniSeRespondeConTodosSusDatos(mapper.toResponse(MiniObjetivoItem.de(miniObjetivo())));
    }

    @Test
    @DisplayName("Mapea la lista de mini-objetivos con su historia y lo que falta")
    void mapeaLaListaConSuHistoriaYLoQueFalta() {
        entoncesLaListaTiene(mapper.toResponse(new ListarMiniObjetivosResponse(
                List.of(MiniObjetivoItem.de(miniObjetivo())),
                List.of(MiniObjetivoItem.de(miniObjetivo()), MiniObjetivoItem.de(miniObjetivo())),
                FaltanDatosMiniObjetivos.POCOS_DATOS)), 1, 2, FaltanDatosMiniObjetivos.POCOS_DATOS);
    }

    @Test
    @DisplayName("Mapea las alternativas con todos sus datos")
    void mapeaLasAlternativasConTodosSusDatos() {
        entoncesLaUnicaAlternativaTieneTodosSusDatos(mapper.toResponse(ListarCandidatosResponse.de(List.of(candidato()))));
    }

    @Test
    @DisplayName("Sin alternativas devuelve una lista vacía")
    void sinAlternativasDevuelveUnaListaVacia() {
        entoncesNoHayAlternativas(mapper.toResponse(ListarCandidatosResponse.de(List.of())));
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private static void entoncesElMiniSeRespondeConTodosSusDatos(MiniObjetivoResponse mini) {
        assertThat(mini.id()).isEqualTo(MINI_ID);
        assertThat(mini.posicion()).isEqualTo(1);
        assertThat(mini.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(mini.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
        assertThat(mini.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(mini.titulo()).isEqualTo(TITULO);
        assertThat(mini.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(mini.referencia()).isEqualTo(PRODUCTO);
        assertThat(mini.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(mini.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(mini.valorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(mini.meta()).isEqualTo(META_MINI);
        assertThat(mini.avance()).isEqualByComparingTo("0");
        assertThat(mini.aporteEstimado()).isEqualTo(APORTE);
        assertThat(mini.semanaInicio()).isEqualTo(SEMANA);
        assertThat(mini.textoConIa()).isFalse();
    }

    private static void entoncesLaListaTiene(MiniObjetivosResponse lista, int minis, int historial,
                                             FaltanDatosMiniObjetivos faltan) {
        assertThat(lista.minis()).hasSize(minis);
        assertThat(lista.historial()).hasSize(historial);
        assertThat(lista.faltanDatos()).isEqualTo(faltan);
    }

    private static void entoncesLaUnicaAlternativaTieneTodosSusDatos(List<CandidatoMiniObjetivoResponse> alternativas) {
        assertThat(alternativas).containsExactly(new CandidatoMiniObjetivoResponse(TipoMiniObjetivo.SUBIR_PRECIO,
                SentidoMiniObjetivo.SUBE, TITULO, JUSTIFICACION, PRODUCTO, PRODUCTO_ID, VALOR_INICIAL, META_MINI, APORTE));
    }

    private static void entoncesNoHayAlternativas(List<CandidatoMiniObjetivoResponse> alternativas) {
        assertThat(alternativas).isEmpty();
    }
}