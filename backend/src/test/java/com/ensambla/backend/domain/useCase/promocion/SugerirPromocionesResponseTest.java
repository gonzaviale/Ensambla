package com.ensambla.backend.domain.useCase.promocion;

import com.ensambla.backend.domain.model.promocion.CanalInstagram;
import com.ensambla.backend.domain.model.promocion.CanalPromocion;
import com.ensambla.backend.domain.model.promocion.ProductoPromocionado;
import com.ensambla.backend.domain.model.promocion.Promocion;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SugerirPromocionesResponseTest {

    @Test
    @DisplayName("Copia cada promoción con sus productos y cada canal con su clave y su nombre")
    void deShouldCopyPromotionsAndChannels() {
        Promocion promocion = new Promocion("estrella", "Tu producto estrella", "Lo más vendido",
                List.of(new ProductoPromocionado("Yerba", new BigDecimal("3600"))));
        List<CanalPromocion> canales = List.of(new CanalInstagram());

        SugerirPromocionesResponse result = SugerirPromocionesResponse.de(List.of(promocion), canales);

        assertThat(result.promociones()).singleElement().satisfies(item -> {
            assertThat(item.clave()).isEqualTo("estrella");
            assertThat(item.titulo()).isEqualTo("Tu producto estrella");
            assertThat(item.gancho()).isEqualTo("Lo más vendido");
            assertThat(item.productos()).singleElement().satisfies(producto -> {
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
    @DisplayName("Sin promociones ni canales las listas quedan vacías")
    void deShouldReturnEmptyLists() {
        SugerirPromocionesResponse result = SugerirPromocionesResponse.de(List.of(), List.of());

        assertThat(result.promociones()).isEmpty();
        assertThat(result.canales()).isEmpty();
    }
}
