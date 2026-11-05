package com.ensambla.backend.infrastructure.presentation.mapper.promocion;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest.ProductoARedactar;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionResponse;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.CanalItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.ProductoPromocionadoItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.PromocionItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.ProductoPromocionWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.RedactarPromocionWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.SugerenciasDePromocionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.TextoDePromocionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PromocionDePrueba.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PromocionPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final PromocionPresentationMapper mapper = new PromocionPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de sugerencias con el usuario de la sesión y el sistema")
    void toSugerirRequestShouldCarryTheUserAndTheProject() {
        SugerirPromocionesRequest result = mapper.toSugerirRequest(USUARIO_ID, PROYECTO_ID);

        assertThat(result).isEqualTo(new SugerirPromocionesRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Copia cada promoción con sus productos y cada canal")
    void toResponseShouldCopyPromotionsAndChannels() {
        SugerirPromocionesResponse response = new SugerirPromocionesResponse(
                List.of(new PromocionItem("estrella", "Tu producto estrella", "Lo más vendido",
                        List.of(new ProductoPromocionadoItem("Yerba", new BigDecimal("3600"))))),
                List.of(new CanalItem("instagram", "Instagram")));

        SugerenciasDePromocionResponse result = mapper.toResponse(response);

        assertThat(result.promociones()).singleElement().satisfies(promocion -> {
            assertThat(promocion.clave()).isEqualTo("estrella");
            assertThat(promocion.titulo()).isEqualTo("Tu producto estrella");
            assertThat(promocion.gancho()).isEqualTo("Lo más vendido");
            assertThat(promocion.productos()).singleElement().satisfies(producto -> {
                assertThat(producto.nombre()).isEqualTo("Yerba");
                assertThat(producto.precio()).isEqualByComparingTo("3600");
            });
        });
        assertThat(result.canales()).singleElement().satisfies(canal -> {
            assertThat(canal.clave()).isEqualTo("instagram");
            assertThat(canal.nombre()).isEqualTo("Instagram");
        });
    }

    @Test
    @DisplayName("Arma el pedido de redacción con el canal, la promoción y sus productos")
    void toRedactarRequestShouldCarryTheChannelAndThePromotion() {
        RedactarPromocionWebRequest request = new RedactarPromocionWebRequest("estado", "estrella", "Título", "Gancho",
                List.of(new ProductoPromocionWebRequest("Yerba", new BigDecimal("3600"))));

        RedactarPromocionRequest result = mapper.toRedactarRequest(USUARIO_ID, request);

        assertThat(result).isEqualTo(new RedactarPromocionRequest(USUARIO_ID, "estado", "estrella", "Título", "Gancho",
                List.of(new ProductoARedactar("Yerba", new BigDecimal("3600")))));
    }

    @Test
    @DisplayName("Devuelve el texto escrito")
    void toResponseShouldReturnTheText() {
        TextoDePromocionResponse result = mapper.toResponse(new RedactarPromocionResponse("Hola"));

        assertThat(result.texto()).isEqualTo("Hola");
    }
}
