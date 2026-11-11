package com.ensambla.backend.domain.useCase.objetivo.obtenerPasos;

import com.ensambla.backend.domain.model.objetivo.PasoObjetivo;
import com.ensambla.backend.domain.model.objetivo.PasoObjetivo.PrecioZona;
import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PasoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObtenerPasosResponseTest {

    @Test
    @DisplayName("Copia todos los datos de cada paso, incluido el rango de zona")
    void deShouldCopyEveryStepFieldIncludingZoneRange() {
        PasoObjetivo paso = new PasoObjetivo(
                TipoPasoObjetivo.SUBIR_PRECIO,
                "Yerba",
                decimal("3600"),
                decimal("2700"),
                decimal("4"),
                decimal("60"),
                new PrecioZona(decimal("3500"), decimal("3900"), decimal("4300")));

        ObtenerPasosResponse response = ObtenerPasosResponse.de(List.of(paso));

        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.tipo()).isEqualTo(TipoPasoObjetivo.SUBIR_PRECIO);
            assertThat(item.nombre()).isEqualTo("Yerba");
            assertThat(item.precio()).isEqualByComparingTo("3600");
            assertThat(item.costo()).isEqualByComparingTo("2700");
            assertThat(item.stock()).isEqualByComparingTo("4");
            assertThat(item.vendidas30Dias()).isEqualByComparingTo("60");
            assertThat(item.precioZona().minimo()).isEqualByComparingTo("3500");
            assertThat(item.precioZona().promedio()).isEqualByComparingTo("3900");
            assertThat(item.precioZona().maximo()).isEqualByComparingTo("4300");
        });
    }

    @Test
    @DisplayName("Conserva el precio de zona vacío y admite una lista sin pasos")
    void deShouldKeepMissingZoneAndAllowEmptyList() {
        PasoObjetivo paso = new PasoObjetivo(
                TipoPasoObjetivo.PONER_A_LA_VISTA,
                "Alfajor",
                decimal("1000"),
                decimal("400"),
                decimal("8"),
                decimal("10"),
                null);

        PasoItem item = ObtenerPasosResponse.de(List.of(paso)).content().getFirst();

        assertThat(item.precioZona()).isNull();
        assertThat(ObtenerPasosResponse.de(List.of()).content()).isEmpty();
    }

    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
