package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PasoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PrecioZonaItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PasoObjetivoWebResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final ObjetivoPresentationMapper mapper = new ObjetivoPresentationMapper();

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
