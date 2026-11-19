package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PruebaPremiumTest {

    private PruebaPremium prueba;
    private PruebaPremium resultado;
    private Throwable excepcion;

    @Test
    @DisplayName("La prueba dura 168 horas exactas y comienza sin consumo")
    void laPruebaDura168HorasExactasYComienzaSinConsumo() {
        cuandoSeIniciaUnaPrueba();

        entoncesDura168HorasExactasYComienzaSinConsumo();
    }

    @Test
    @DisplayName("Las páginas admitidas se acumulan hasta diez sin renovar la fecha ni el contador")
    void lasPaginasAdmitidasSeAcumulanHastaDiezSinRenovarLaFechaNiElContador() {
        dadoUnaPruebaConPaginasUsadas(8);

        cuandoSeConsumenPaginas(2);

        entoncesElResultadoAcumulaDiezPaginasSinRenovarLasFechas();
        entoncesLaPruebaOriginalConservaSusOchoPaginas();
    }

    @Test
    @DisplayName("Se rechaza completo el intento que supera las diez páginas")
    void seRechazaCompletoElIntentoQueSuperaLasDiezPaginas() {
        dadoUnaPruebaConPaginasUsadas(9);

        cuandoSeValidanPaginas(2);

        entoncesSeRechazaPorElLimiteDeIaDeLaPrueba();
    }

    // --- dado ---
    private void dadoUnaPruebaConPaginasUsadas(int paginas) {
        prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), paginas);
    }

    // --- cuando ---
    private void cuandoSeIniciaUnaPrueba() {
        resultado = PruebaPremium.nueva(NEGOCIO_ID, INSTANTE);
    }

    private void cuandoSeConsumenPaginas(int paginas) {
        resultado = prueba.consumir(paginas);
    }

    private void cuandoSeValidanPaginas(int paginas) {
        excepcion = catchThrowable(() -> prueba.validarPaginas(paginas));
    }

    // --- entonces ---
    private void entoncesDura168HorasExactasYComienzaSinConsumo() {
        assertThat(resultado.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(resultado.inicioEn()).isEqualTo(INSTANTE);
        assertThat(Duration.between(resultado.inicioEn(), resultado.finEn())).isEqualTo(Duration.ofHours(168));
        assertThat(resultado.paginasIa()).isZero();
    }

    private void entoncesElResultadoAcumulaDiezPaginasSinRenovarLasFechas() {
        assertThat(resultado.paginasIa()).isEqualTo(10);
        assertThat(resultado.inicioEn()).isEqualTo(INSTANTE);
        assertThat(resultado.finEn()).isEqualTo(INSTANTE.plusDays(7));
    }

    private void entoncesLaPruebaOriginalConservaSusOchoPaginas() {
        assertThat(prueba.paginasIa()).isEqualTo(8);
    }

    private void entoncesSeRechazaPorElLimiteDeIaDeLaPrueba() {
        assertThat(excepcion).isInstanceOf(RestriccionPlanException.class)
                .extracting("codigo").isEqualTo("LIMITE_IA_PRUEBA");
    }
}
