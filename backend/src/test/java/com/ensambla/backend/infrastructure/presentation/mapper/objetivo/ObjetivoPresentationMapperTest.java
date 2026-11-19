package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
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
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoPresentationMapperTest {

    private static final PasoItem SUBIR_LA_YERBA = new PasoItem(TipoPasoObjetivo.SUBIR_PRECIO, "Yerba",
            decimal("3600"), decimal("2700"), decimal("4"), decimal("60"),
            new PrecioZonaItem(decimal("3500"), decimal("3900"), decimal("4300")));
    private static final PasoItem ALFAJOR_SIN_ZONA = new PasoItem(TipoPasoObjetivo.PONER_A_LA_VISTA, "Alfajor",
            decimal("1000"), decimal("400"), decimal("8"), decimal("10"), null);

    private final ObjetivoPresentationMapper mapper = new ObjetivoPresentationMapper();
    private List<PasoObjetivoWebResponse> pasos;

    @Test
    @DisplayName("Arma el pedido de listado con el usuario de la sesión")
    void armaElPedidoDeListado() {
        entoncesElPedidoEs(mapper.toListarRequest(USUARIO_ID), new ListarObjetivosRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de alta con el usuario de la sesión y el texto y la meta del body")
    void armaElPedidoDeAlta() {
        entoncesElPedidoEs(mapper.toCrearRequest(USUARIO_ID, new CrearObjetivoWebRequest(TEXTO, META)),
                new CrearObjetivoRequest(USUARIO_ID, TEXTO, META));
    }

    @Test
    @DisplayName("Arma el pedido de baja con el usuario de la sesión y el id de la ruta")
    void armaElPedidoDeBaja() {
        entoncesElPedidoEs(mapper.toEliminarRequest(USUARIO_ID, OBJETIVO_ID), new EliminarObjetivoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de los pasos con el usuario autenticado")
    void armaElPedidoDeLosPasos() {
        entoncesElPedidoEs(mapper.toObtenerPasosRequest(USUARIO_ID), new ObtenerPasosRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("Mapea cada campo del item a la respuesta")
    void mapeaCadaCampoDelItem() {
        entoncesElObjetivoSeRespondeComo(new ObjetivoResponse(OBJETIVO_ID, TEXTO, META, AHORA));
    }

    @Test
    @DisplayName("Mapea una lista de items conservando el orden")
    void mapeaUnaListaConservandoElOrden() {
        entoncesLaListaTieneLosObjetivos(List.of(ObjetivoItem.de(objetivo())), OBJETIVO_ID);
    }

    @Test
    @DisplayName("Una lista vacía se mapea a una lista vacía")
    void unaListaVaciaSeMapeaVacia() {
        entoncesLaListaTieneLosObjetivos(List.of());
    }

    @Test
    @DisplayName("Mapea todos los campos de los pasos y su rango de zona")
    void mapeaTodosLosCamposDeLosPasos() {
        cuandoSeRespondenLosPasos(SUBIR_LA_YERBA);

        entoncesElUnicoPasoEsSubirElPrecioDeLaYerba();
    }

    @Test
    @DisplayName("Conserva la zona vacía de un paso")
    void conservaLaZonaVaciaDeUnPaso() {
        cuandoSeRespondenLosPasos(ALFAJOR_SIN_ZONA);

        entoncesElPrimerPasoNoTieneZona();
    }

    @Test
    @DisplayName("Sin pasos devuelve una lista vacía")
    void sinPasosDevuelveUnaListaVacia() {
        cuandoSeRespondenLosPasos();

        entoncesNoHayPasos();
    }

    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }

    private void cuandoSeRespondenLosPasos(PasoItem... items) {
        pasos = mapper.toResponse(new ObtenerPasosResponse(List.of(items)));
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private void entoncesElObjetivoSeRespondeComo(ObjetivoResponse esperado) {
        assertThat(mapper.toResponse(ObjetivoItem.de(objetivo()))).isEqualTo(esperado);

    }

    private void entoncesLaListaTieneLosObjetivos(List<ObjetivoItem> objetivos, ObjetivoId... ids) {
        assertThat(mapper.toResponse(objetivos)).extracting(ObjetivoResponse::id).containsExactly(ids);
    }

    private void entoncesElUnicoPasoEsSubirElPrecioDeLaYerba() {
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

    private void entoncesElPrimerPasoNoTieneZona() {
        assertThat(pasos.getFirst().precioZona()).isNull();
    }

    private void entoncesNoHayPasos() {
        assertThat(pasos).isEmpty();
    }
}