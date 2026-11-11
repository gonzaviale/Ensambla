package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

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

    private final ObjetivoPresentationMapper mapper = new ObjetivoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listado con el usuario de la sesión")
    void toListarRequestShouldCarrySessionUsuario() {
        ListarObjetivosRequest result = mapper.toListarRequest(USUARIO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Arma el pedido de alta con el usuario de la sesión y el texto y la meta del body")
    void toCrearRequestShouldCarryUsuarioAndBody() {
        CrearObjetivoRequest result = mapper.toCrearRequest(USUARIO_ID, new CrearObjetivoWebRequest(TEXTO, META));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.texto()).isEqualTo(TEXTO);
        assertThat(result.meta()).isEqualTo(META);
    }

    @Test
    @DisplayName("Arma el pedido de baja con el usuario de la sesión y el id de la ruta")
    void toEliminarRequestShouldCarryUsuarioAndId() {
        EliminarObjetivoRequest result = mapper.toEliminarRequest(USUARIO_ID, OBJETIVO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Mapea cada campo del item a la respuesta")
    void toResponseShouldMapEveryField() {
        ObjetivoResponse result = mapper.toResponse(ObjetivoItem.de(objetivo()));

        assertThat(result.id()).isEqualTo(OBJETIVO_ID);
        assertThat(result.texto()).isEqualTo(TEXTO);
        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea una lista de items conservando el orden")
    void toResponseShouldMapList() {
        List<ObjetivoResponse> result = mapper.toResponse(List.of(ObjetivoItem.de(objetivo())));

        assertThat(result).extracting(ObjetivoResponse::id).containsExactly(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Una lista vacía se mapea a una lista vacía")
    void toResponseShouldMapEmptyList() {
        List<ObjetivoResponse> result = mapper.toResponse(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Arma el pedido con el usuario autenticado")
    void toObtenerPasosRequestShouldUseAuthenticatedUser() {
        ObtenerPasosRequest request = mapper.toObtenerPasosRequest(USUARIO_ID);

        assertThat(request.usuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Mapea todos los campos de los pasos y su rango de zona")
    void toResponseShouldMapEveryStepAndZoneField() {
        ObtenerPasosResponse response = new ObtenerPasosResponse(List.of(new PasoItem(
                TipoPasoObjetivo.SUBIR_PRECIO,
                "Yerba",
                decimal("3600"),
                decimal("2700"),
                decimal("4"),
                decimal("60"),
                new PrecioZonaItem(decimal("3500"), decimal("3900"), decimal("4300")))));

        List<PasoObjetivoWebResponse> result = mapper.toResponse(response);

        assertThat(result).singleElement().satisfies(paso -> {
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

    @Test
    @DisplayName("Conserva la zona vacía y devuelve una lista vacía sin pasos")
    void toResponseShouldKeepMissingZoneAndAllowEmptyList() {
        PasoItem item = new PasoItem(
                TipoPasoObjetivo.PONER_A_LA_VISTA,
                "Alfajor",
                decimal("1000"),
                decimal("400"),
                decimal("8"),
                decimal("10"),
                null);

        List<PasoObjetivoWebResponse> result = mapper.toResponse(new ObtenerPasosResponse(List.of(item)));

        assertThat(result.getFirst().precioZona()).isNull();
        assertThat(mapper.toResponse(new ObtenerPasosResponse(List.of()))).isEmpty();
    }

    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
