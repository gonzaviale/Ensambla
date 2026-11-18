package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PasoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PrecioZonaItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CrearObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PasoObjetivoWebResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoPresentationMapperTest {

    private final ObjetivoPresentationMapper mapper = new ObjetivoPresentationMapper();
    private ListarObjetivosRequest pedidoDeListado;
    private CrearObjetivoRequest pedidoDeAlta;
    private EliminarObjetivoRequest pedidoDeBaja;
    private ObtenerPasosRequest pedidoDePasos;
    private ObjetivoResponse respuesta;
    private List<ObjetivoResponse> respuestas;
    private List<PasoObjetivoWebResponse> pasos;

    @Test
    @DisplayName("Arma el pedido de listado con el usuario de la sesión")
    void armaElPedidoDeListadoConElUsuarioDeLaSesion() {
        cuandoSeArmaElPedidoDeListado();

        entoncesElPedidoDeListadoTraeElUsuario();
    }

    @Test
    @DisplayName("Arma el pedido de alta con el usuario de la sesión y el texto y la meta del body")
    void armaElPedidoDeAltaConElUsuarioDeLaSesionYElTextoYLaMetaDelBody() {
        cuandoSeArmaElPedidoDeAlta();

        entoncesElPedidoDeAltaTraeUsuarioTextoYMeta();
    }

    @Test
    @DisplayName("Arma el pedido de baja con el usuario de la sesión y el id de la ruta")
    void armaElPedidoDeBajaConElUsuarioDeLaSesionYElIdDeLaRuta() {
        cuandoSeArmaElPedidoDeBaja();

        entoncesElPedidoDeBajaTraeUsuarioEId();
    }

    @Test
    @DisplayName("Mapea cada campo del item a la respuesta")
    void mapeaCadaCampoDelItemALaRespuesta() {
        cuandoSeMapeaUnObjetivo();

        entoncesSeMapeaCadaCampoDelObjetivo();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami se responde sin meta y con su tipo")
    void unObjetivoArmadoConSamiSeRespondeSinMetaYConSuTipo() {
        cuandoSeMapeaUnObjetivoDeSami();

        entoncesSeRespondeSinMetaYConSuTipo();
    }

    @Test
    @DisplayName("Mapea una lista de items conservando el orden")
    void mapeaUnaListaDeItemsConservandoElOrden() {
        cuandoSeMapeaUnaLista(List.of(ObjetivoItem.de(objetivo())));

        entoncesLosIdsSon(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Una lista vacía se mapea a una lista vacía")
    void unaListaVaciaSeMapeaAUnaListaVacia() {
        cuandoSeMapeaUnaLista(List.of());

        entoncesNoHayRespuestas();
    }

    @Test
    @DisplayName("Arma el pedido con el usuario autenticado")
    void armaElPedidoConElUsuarioAutenticado() {
        cuandoSeArmaElPedidoDePasos();

        entoncesElPedidoDePasosTraeElUsuario();
    }

    @Test
    @DisplayName("Mapea todos los campos de los pasos y su rango de zona")
    void mapeaTodosLosCamposDeLosPasosYSuRangoDeZona() {
        cuandoSeMapeanLosPasos(pasosConZona());

        entoncesSeMapeanTodosLosCamposDelPasoYSuZona();
    }

    @Test
    @DisplayName("Conserva la zona vacía de un paso")
    void conservaLaZonaVaciaDeUnPaso() {
        cuandoSeMapeanLosPasos(pasosSinZona());

        entoncesElPasoNoTieneZona();
    }

    @Test
    @DisplayName("Sin pasos devuelve una lista vacía")
    void sinPasosDevuelveUnaListaVacia() {
        cuandoSeMapeanLosPasos(sinPasos());

        entoncesNoHayPasos();
    }

    // --- act ---
    private void cuandoSeArmaElPedidoDeListado() {
        pedidoDeListado = mapper.toListarRequest(USUARIO_ID);
    }

    private void cuandoSeArmaElPedidoDeAlta() {
        pedidoDeAlta = mapper.toCrearRequest(USUARIO_ID, new CrearObjetivoWebRequest(TEXTO, META));
    }

    private void cuandoSeArmaElPedidoDeBaja() {
        pedidoDeBaja = mapper.toEliminarRequest(USUARIO_ID, OBJETIVO_ID);
    }

    private void cuandoSeMapeaUnObjetivo() {
        respuesta = mapper.toResponse(ObjetivoItem.de(objetivo()));
    }

    private void cuandoSeMapeaUnObjetivoDeSami() {
        respuesta = mapper.toResponse(ObjetivoItem.de(objetivoSinMeta()));
    }

    private void cuandoSeMapeaUnaLista(List<ObjetivoItem> items) {
        respuestas = mapper.toResponse(items);
    }

    private void cuandoSeArmaElPedidoDePasos() {
        pedidoDePasos = mapper.toObtenerPasosRequest(USUARIO_ID);
    }

    private void cuandoSeMapeanLosPasos(ObtenerPasosResponse response) {
        pasos = mapper.toResponse(response);
    }

    // --- assert ---
    private void entoncesElPedidoDeListadoTraeElUsuario() {
        assertThat(pedidoDeListado.usuarioId()).isEqualTo(USUARIO_ID);
    }

    private void entoncesElPedidoDeAltaTraeUsuarioTextoYMeta() {
        assertThat(pedidoDeAlta.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeAlta.texto()).isEqualTo(TEXTO);
        assertThat(pedidoDeAlta.meta()).isEqualTo(META);
    }

    private void entoncesElPedidoDeBajaTraeUsuarioEId() {
        assertThat(pedidoDeBaja.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeBaja.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeMapeaCadaCampoDelObjetivo() {
        assertThat(respuesta.id()).isEqualTo(OBJETIVO_ID);
        assertThat(respuesta.texto()).isEqualTo(TEXTO);
        assertThat(respuesta.tipo()).isEqualTo(TipoObjetivo.MONETARIO);
        assertThat(respuesta.meta()).isEqualTo(META);
        assertThat(respuesta.armadoConSami()).isFalse();
        assertThat(respuesta.creadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeRespondeSinMetaYConSuTipo() {
        assertThat(respuesta.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(respuesta.meta()).isNull();
        assertThat(respuesta.armadoConSami()).isTrue();
    }

    private void entoncesLosIdsSon(ObjetivoId... ids) {
        assertThat(respuestas).extracting(ObjetivoResponse::id).containsExactly(ids);
    }

    private void entoncesNoHayRespuestas() {
        assertThat(respuestas).isEmpty();
    }

    private void entoncesElPedidoDePasosTraeElUsuario() {
        assertThat(pedidoDePasos.usuarioId()).isEqualTo(USUARIO_ID);
    }

    private void entoncesSeMapeanTodosLosCamposDelPasoYSuZona() {
        assertThat(pasos).singleElement().satisfies(paso -> {
            assertThat(paso.tipo()).isEqualTo(TipoPasoObjetivo.SUBIR_PRECIO);
            assertThat(paso.nombre()).isEqualTo("Yerba");
            assertThat(paso.precio()).isEqualByComparingTo("3600");
            assertThat(paso.costo()).isEqualByComparingTo("2700");
            assertThat(paso.stock()).isEqualByComparingTo("4");
            assertThat(paso.vendidas30Dias()).isEqualByComparingTo("60");
            assertThat(paso.precioZona().minimo()).isEqualByComparingTo("3500");
            assertThat(paso.precioZona().promedio()).isEqualByComparingTo("3900");
            assertThat(paso.precioZona().maximo()).isEqualByComparingTo("4300");
        });
    }

    private void entoncesElPasoNoTieneZona() {
        assertThat(pasos.getFirst().precioZona()).isNull();
    }

    private void entoncesNoHayPasos() {
        assertThat(pasos).isEmpty();
    }

    // --- helpers ---
    private static PasoItem pasoConZona() {
        return new PasoItem(TipoPasoObjetivo.SUBIR_PRECIO, "Yerba", decimal("3600"), decimal("2700"), decimal("4"),
                decimal("60"), new PrecioZonaItem(decimal("3500"), decimal("3900"), decimal("4300")));
    }

    private static PasoItem pasoSinZona() {
        return new PasoItem(TipoPasoObjetivo.PONER_A_LA_VISTA, "Alfajor", decimal("1000"), decimal("400"),
                decimal("8"), decimal("10"), null);
    }

    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }

    private static ObtenerPasosResponse pasosConZona() {
        return new ObtenerPasosResponse(List.of(pasoConZona()));
    }

    private static ObtenerPasosResponse pasosSinZona() {
        return new ObtenerPasosResponse(List.of(pasoSinZona()));
    }

    private static ObtenerPasosResponse sinPasos() {
        return new ObtenerPasosResponse(List.of());
    }
}
