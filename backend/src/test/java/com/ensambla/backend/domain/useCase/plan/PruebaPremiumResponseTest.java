package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PruebaPremiumResponseTest {

    private PruebaPremium prueba;
    private PruebaPremiumResponse respuesta;

    @Test
    @DisplayName("Mapea fechas, páginas utilizadas y límite total de prueba")
    void mapeaFechasPaginasUtilizadasYLimiteTotalDePrueba() {
        dadoUnaPruebaConTresPaginasUsadas();

        cuandoSeArmaLaRespuesta();

        entoncesCopiaLasFechasLasPaginasUsadasYElLimiteTotal();
    }

    // --- dado ---
    private void dadoUnaPruebaConTresPaginasUsadas() {
        prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 3);
    }

    // --- cuando ---
    private void cuandoSeArmaLaRespuesta() {
        respuesta = PruebaPremiumResponse.de(prueba);
    }

    // --- entonces ---
    private void entoncesCopiaLasFechasLasPaginasUsadasYElLimiteTotal() {
        assertThat(respuesta.inicioEn()).isEqualTo(INSTANTE);
        assertThat(respuesta.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(respuesta.paginasIaUtilizadas()).isEqualTo(3);
        assertThat(respuesta.limitePaginasIa()).isEqualTo(10);
    }
}
