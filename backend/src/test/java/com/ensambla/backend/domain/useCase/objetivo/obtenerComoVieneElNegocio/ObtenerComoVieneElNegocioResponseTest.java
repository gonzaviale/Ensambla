package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioResponse.Semana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObtenerComoVieneElNegocioResponseTest {

    private ObtenerComoVieneElNegocioResponse respuesta;

    @Test
    @DisplayName("Copia los importes y las semanas del modelo")
    void copiaLosImportesYLasSemanasDelModelo() {
        cuandoSeArmaLaRespuestaDeUnNegocioConDosSemanas();

        entoncesCopiaLosImportesYLasSemanas();
    }

    // --- cuando ---

    private void cuandoSeArmaLaRespuestaDeUnNegocioConDosSemanas() {
        ComoVieneElNegocio negocio = new ComoVieneElNegocio(true, 56100, 40500, 8000, 7600, 17000,
                List.of(new ComoVieneElNegocio.Semana("Hace 1 sem.", 4100),
                        new ComoVieneElNegocio.Semana("Hoy", 7600)));
        respuesta = ObtenerComoVieneElNegocioResponse.de(negocio);
    }

    // --- entonces ---

    private void entoncesCopiaLosImportesYLasSemanas() {
        assertThat(respuesta).isEqualTo(new ObtenerComoVieneElNegocioResponse(true, 56100, 40500, 8000, 7600, 17000,
                List.of(new Semana("Hace 1 sem.", 4100), new Semana("Hoy", 7600))));
    }
}
