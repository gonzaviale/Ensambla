package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.MiniProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ObtenerProgresoResponseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
    private static final LocalDate DIA = LocalDate.of(2026, 11, 11);
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private PuntoSemanal punto;
    private MiniProgreso miniProgreso;
    private ObtenerProgresoResponse respuesta;
    private List<PuntoSemanal> semanas;

    @Test
    @DisplayName("Un punto semanal toma el valor de la foto y la semana de su día, como medido")
    void unPuntoSemanalTomaElValorDeLaFotoYLaSemanaDeSuDiaComoMedido() {
        cuandoSeArmaElPuntoSemanalDeUnaFoto();

        entoncesElPuntoEsDeLaSemanaDeLaFotoYEstaMedido();
    }

    @Test
    @DisplayName("El progreso de un mini-objetivo trae su ficha, su hito y la última foto de cada semana")
    void elProgresoDeUnMiniObjetivoTraeSuFichaSuHitoYLaUltimaFotoDeCadaSemana() {
        cuandoSeArmaElProgresoDeUnMiniObjetivoLogradoConDosFotosEnLaMismaSemana();

        entoncesTraeSuFichaSuHitoYLaUltimaFotoDeLaSemana();
    }

    @Test
    @DisplayName("Un mini-objetivo sin nada para destacar no trae hito")
    void unMiniObjetivoSinNadaParaDestacarNoTraeHito() {
        cuandoSeArmaElProgresoDeUnMiniObjetivoSinFotos();

        entoncesNoTraeHitoNiSemanas();
    }

    @Test
    @DisplayName("La respuesta copia la meta y el hito del objetivo")
    void laRespuestaCopiaLaMetaYElHitoDelObjetivo() {
        cuandoSeArmaLaRespuestaDeUnObjetivoAlcanzado();

        entoncesCopiaLaMetaYElHitoDeObjetivoAlcanzado();
    }

    @Test
    @DisplayName("Un objetivo que no llegó a la meta no trae hito")
    void unObjetivoQueNoLlegoALaMetaNoTraeHito() {
        cuandoSeArmaLaRespuestaDeUnObjetivoSinAlcanzar();

        entoncesLaRespuestaNoTraeHito();
    }

    // --- cuando ---

    private void cuandoSeArmaElPuntoSemanalDeUnaFoto() {
        punto = PuntoSemanal.de(ProgresoObjetivo.de(OBJETIVO_ID, null, DIA, BigDecimal.TEN, AHORA));
    }

    private void cuandoSeArmaElProgresoDeUnMiniObjetivoLogradoConDosFotosEnLaMismaSemana() {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(EstadoMiniObjetivo.LOGRADO);
        List<ProgresoObjetivo> fotos = List.of(
                ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, DIA.minusDays(1), BigDecimal.ONE, AHORA),
                ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, DIA, BigDecimal.TEN, AHORA));
        miniProgreso = MiniProgreso.de(mini, HOY, fotos);
    }

    private void cuandoSeArmaElProgresoDeUnMiniObjetivoSinFotos() {
        miniProgreso = MiniProgreso.de(miniObjetivo(), HOY, List.of());
    }

    private void cuandoSeArmaLaRespuestaDeUnObjetivoAlcanzado() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        semanas = List.of(new PuntoSemanal(SEMANA, BigDecimal.TEN, true));
        respuesta = ObtenerProgresoResponse.de(alcanzado, semanas, List.of());
    }

    private void cuandoSeArmaLaRespuestaDeUnObjetivoSinAlcanzar() {
        respuesta = ObtenerProgresoResponse.de(objetivo(), List.of(), List.of());
    }

    // --- entonces ---

    private void entoncesElPuntoEsDeLaSemanaDeLaFotoYEstaMedido() {
        assertThat(punto.semana()).isEqualTo(SEMANA);
        assertThat(punto.valor()).isEqualTo(BigDecimal.TEN);
        assertThat(punto.estimado()).isFalse();
    }

    private void entoncesTraeSuFichaSuHitoYLaUltimaFotoDeLaSemana() {
        assertThat(miniProgreso.mini().id()).isEqualTo(MINI_ID);
        assertThat(miniProgreso.hito()).isEqualTo(HitoObjetivo.MINI_LOGRADO);
        assertThat(miniProgreso.semanas()).singleElement()
                .satisfies(unPunto -> assertThat(unPunto.valor()).isEqualTo(BigDecimal.TEN));
    }

    private void entoncesNoTraeHitoNiSemanas() {
        assertThat(miniProgreso.hito()).isNull();
        assertThat(miniProgreso.semanas()).isEmpty();
    }

    private void entoncesCopiaLaMetaYElHitoDeObjetivoAlcanzado() {
        assertThat(respuesta.meta()).isEqualTo(META);
        assertThat(respuesta.hito()).isEqualTo(HitoObjetivo.OBJETIVO_ALCANZADO);
        assertThat(respuesta.semanas()).isEqualTo(semanas);
        assertThat(respuesta.minis()).isEmpty();
    }

    private void entoncesLaRespuestaNoTraeHito() {
        assertThat(respuesta.hito()).isNull();
    }
}
