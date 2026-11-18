package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
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
    private ListarMiniObjetivosRequest pedidoDeListar;
    private ListarCandidatosRequest pedidoDeCandidatos;
    private ReemplazarMiniObjetivoRequest pedidoDeReemplazo;
    private AbandonarMiniObjetivoRequest pedidoDeAbandonar;
    private MiniObjetivoResponse respuestaDelMini;
    private MiniObjetivosResponse respuestaDeLaLista;
    private List<CandidatoMiniObjetivoResponse> alternativas;

    @Test
    @DisplayName("Arma el pedido de listar con el usuario de la sesión y el objetivo de la ruta")
    void armaElPedidoDeListarConElUsuarioDeLaSesionYElObjetivoDeLaRuta() {
        cuandoSeArmaElPedidoDeListar();

        entoncesElPedidoDeListarTraeUsuarioYObjetivo();
    }

    @Test
    @DisplayName("Arma el pedido de candidatos con el usuario, el objetivo y el mini-objetivo")
    void armaElPedidoDeCandidatosConElUsuarioElObjetivoYElMiniObjetivo() {
        cuandoSeArmaElPedidoDeCandidatos();

        entoncesElPedidoDeCandidatosTraeLosIds();
    }

    @Test
    @DisplayName("Arma el pedido de reemplazo con los ids de la ruta y el candidato elegido del cuerpo")
    void armaElPedidoDeReemplazoConLosIdsDeLaRutaYElCandidatoElegidoDelCuerpo() {
        cuandoSeArmaElPedidoDeReemplazo();

        entoncesElPedidoDeReemplazoTraeIdsYCandidatoElegido();
    }

    @Test
    @DisplayName("Arma el pedido de abandonar con los ids de la sesión y la ruta")
    void armaElPedidoDeAbandonarConLosIdsDeLaSesionYLaRuta() {
        cuandoSeArmaElPedidoDeAbandonar();

        entoncesElPedidoDeAbandonarTraeLosIds();
    }

    @Test
    @DisplayName("Mapea cada mini-objetivo a su respuesta con todos sus datos")
    void mapeaCadaMiniObjetivoASuRespuestaConTodosSusDatos() {
        cuandoSeMapeaUnMiniObjetivo();

        entoncesSeMapeanTodosLosDatosDelMini();
    }

    @Test
    @DisplayName("Mapea la lista de mini-objetivos con su historia y lo que falta")
    void mapeaLaListaDeMiniObjetivosConSuHistoriaYLoQueFalta() {
        cuandoSeMapeaLaLista();

        entoncesSeMapeanLosMinisElHistorialYLoQueFalta();
    }

    @Test
    @DisplayName("Mapea las alternativas con todos sus datos")
    void mapeaLasAlternativasConTodosSusDatos() {
        cuandoSeMapeanLasAlternativas(List.of(candidato()));

        entoncesSeMapeanTodosLosDatosDeLaAlternativa();
    }

    @Test
    @DisplayName("Sin alternativas devuelve una lista vacía")
    void sinAlternativasDevuelveUnaListaVacia() {
        cuandoSeMapeanLasAlternativas(List.of());

        entoncesNoHayAlternativas();
    }

    // --- act ---
    private void cuandoSeArmaElPedidoDeListar() {
        pedidoDeListar = mapper.toListarRequest(USUARIO_ID, OBJETIVO_ID);
    }

    private void cuandoSeArmaElPedidoDeCandidatos() {
        pedidoDeCandidatos = mapper.toCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID);
    }

    private void cuandoSeArmaElPedidoDeReemplazo() {
        pedidoDeReemplazo = mapper.toReemplazarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID,
                new ReemplazarMiniObjetivoWebRequest(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO));
    }

    private void cuandoSeArmaElPedidoDeAbandonar() {
        pedidoDeAbandonar = mapper.toAbandonarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID);
    }

    private void cuandoSeMapeaUnMiniObjetivo() {
        respuestaDelMini = mapper.toResponse(MiniObjetivoItem.de(miniObjetivo()));
    }

    private void cuandoSeMapeaLaLista() {
        respuestaDeLaLista = mapper.toResponse(new ListarMiniObjetivosResponse(
                List.of(MiniObjetivoItem.de(miniObjetivo())),
                List.of(MiniObjetivoItem.de(miniObjetivo()), MiniObjetivoItem.de(miniObjetivo())),
                FaltanDatosMiniObjetivos.POCOS_DATOS));
    }

    private void cuandoSeMapeanLasAlternativas(List<CandidatoMiniObjetivo> candidatos) {
        alternativas = mapper.toResponse(ListarCandidatosResponse.de(candidatos));
    }

    // --- assert ---
    private void entoncesElPedidoDeListarTraeUsuarioYObjetivo() {
        assertThat(pedidoDeListar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeListar.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesElPedidoDeCandidatosTraeLosIds() {
        assertThat(pedidoDeCandidatos.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeCandidatos.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(pedidoDeCandidatos.miniObjetivoId()).isEqualTo(MINI_ID);
    }

    private void entoncesElPedidoDeReemplazoTraeIdsYCandidatoElegido() {
        assertThat(pedidoDeReemplazo.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeReemplazo.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(pedidoDeReemplazo.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(pedidoDeReemplazo.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(pedidoDeReemplazo.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(pedidoDeReemplazo.referencia()).isEqualTo(PRODUCTO);
    }

    private void entoncesElPedidoDeAbandonarTraeLosIds() {
        assertThat(pedidoDeAbandonar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeAbandonar.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(pedidoDeAbandonar.miniObjetivoId()).isEqualTo(MINI_ID);
    }

    private void entoncesSeMapeanTodosLosDatosDelMini() {
        assertThat(respuestaDelMini.id()).isEqualTo(MINI_ID);
        assertThat(respuestaDelMini.posicion()).isEqualTo(1);
        assertThat(respuestaDelMini.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(respuestaDelMini.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(respuestaDelMini.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
        assertThat(respuestaDelMini.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(respuestaDelMini.titulo()).isEqualTo(TITULO);
        assertThat(respuestaDelMini.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(respuestaDelMini.referencia()).isEqualTo(PRODUCTO);
        assertThat(respuestaDelMini.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(respuestaDelMini.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(respuestaDelMini.valorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(respuestaDelMini.meta()).isEqualTo(META_MINI);
        assertThat(respuestaDelMini.avance()).isEqualByComparingTo("0");
        assertThat(respuestaDelMini.aporteEstimado()).isEqualTo(APORTE);
        assertThat(respuestaDelMini.semanaInicio()).isEqualTo(SEMANA);
        assertThat(respuestaDelMini.textoConIa()).isFalse();
    }

    private void entoncesSeMapeanLosMinisElHistorialYLoQueFalta() {
        assertThat(respuestaDeLaLista.minis()).hasSize(1);
        assertThat(respuestaDeLaLista.historial()).hasSize(2);
        assertThat(respuestaDeLaLista.faltanDatos()).isEqualTo(FaltanDatosMiniObjetivos.POCOS_DATOS);
    }

    private void entoncesSeMapeanTodosLosDatosDeLaAlternativa() {
        assertThat(alternativas).singleElement().satisfies(item -> {
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

    private void entoncesNoHayAlternativas() {
        assertThat(alternativas).isEmpty();
    }
}
