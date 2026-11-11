package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioResponse.Semana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObtenerComoVieneElNegocioResponseTest {

    @Test
    @DisplayName("de() copia los importes y las semanas del modelo")
    void deShouldCopyAmountsAndWeeks() {
        ComoVieneElNegocio negocio = new ComoVieneElNegocio(true, 56100, 40500, 8000, 7600, 17000,
                List.of(new ComoVieneElNegocio.Semana("Hace 1 sem.", 4100), new ComoVieneElNegocio.Semana("Hoy", 7600)));

        ObtenerComoVieneElNegocioResponse response = ObtenerComoVieneElNegocioResponse.de(negocio);

        assertThat(response).isEqualTo(new ObtenerComoVieneElNegocioResponse(true, 56100, 40500, 8000, 7600, 17000,
                List.of(new Semana("Hace 1 sem.", 4100), new Semana("Hoy", 7600))));
    }
}
