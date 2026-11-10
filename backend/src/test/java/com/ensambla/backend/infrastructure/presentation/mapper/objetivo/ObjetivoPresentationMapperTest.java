package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivosRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CrearObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
