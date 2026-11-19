package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.MiniProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProgresoObjetivoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProgresoObjetivoPresentationMapperTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private final ProgresoObjetivoPresentationMapper mapper =
            new ProgresoObjetivoPresentationMapper(new MiniObjetivoPresentationMapper());
    private ProgresoObjetivoResponse progreso;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión y el objetivo de la ruta")
    void armaElPedidoConElUsuarioYElObjetivo() {
        entoncesElPedidoEs(new ObtenerProgresoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    @Test
    @DisplayName("Copia la meta, el hito y la serie del objetivo")
    void copiaLaMetaElHitoYLaSerieDelObjetivo() {
        cuandoSeRespondeElProgresoDeUnaSemana();

        entoncesElObjetivoTiene(META, HitoObjetivo.OBJETIVO_ALCANZADO);
        entoncesLaUnicaSemanaEs(SEMANA, new BigDecimal("-27000"), true);
    }

    @Test
    @DisplayName("Copia la serie de cada mini-objetivo con su hito")
    void copiaLaSerieDeCadaMiniObjetivo() {
        cuandoSeRespondeElProgresoDeUnaSemana();

        entoncesElUnicoMiniTiene(HitoObjetivo.SIN_AVANCE, BigDecimal.TEN, false);
    }

    @Test
    @DisplayName("Sin semanas ni mini-objetivos devuelve listas vacías")
    void sinSemanasNiMinisDevuelveListasVacias() {
        cuandoSeRespondeUnProgresoVacio();

        entoncesElProgresoEstaVacio();
    }

    private void cuandoSeRespondeElProgresoDeUnaSemana() {
        PuntoSemanal punto = new PuntoSemanal(SEMANA, new BigDecimal("-27000"), true);
        MiniProgreso mini = new MiniProgreso(MiniObjetivoItem.de(miniObjetivo()), HitoObjetivo.SIN_AVANCE,
                List.of(new PuntoSemanal(SEMANA, BigDecimal.TEN, false)));
        progreso = mapper.toResponse(
                new ObtenerProgresoResponse(META, HitoObjetivo.OBJETIVO_ALCANZADO, List.of(punto), List.of(mini)));
    }

    private void cuandoSeRespondeUnProgresoVacio() {
        progreso = mapper.toResponse(new ObtenerProgresoResponse(META, null, List.of(), List.of()));
    }

    private void entoncesElPedidoEs(ObtenerProgresoRequest esperado) {
        assertThat(mapper.toRequest(USUARIO_ID, OBJETIVO_ID)).isEqualTo(esperado);
    }

    private void entoncesElObjetivoTiene(BigDecimal meta, HitoObjetivo hito) {
        assertThat(progreso.meta()).isEqualTo(meta);
        assertThat(progreso.hito()).isEqualTo(hito);
    }

    private void entoncesLaUnicaSemanaEs(LocalDate semana, BigDecimal valor, boolean estimado) {
        assertThat(progreso.semanas()).singleElement().satisfies(punto -> {
            assertThat(punto.semana()).isEqualTo(semana);
            assertThat(punto.valor()).isEqualTo(valor);
            assertThat(punto.estimado()).isEqualTo(estimado);
        });
    }

    private void entoncesElUnicoMiniTiene(HitoObjetivo hito, BigDecimal valor, boolean estimado) {
        assertThat(progreso.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.mini().id()).isEqualTo(MINI_ID);
            assertThat(mini.hito()).isEqualTo(hito);
            assertThat(mini.semanas()).singleElement().satisfies(serie -> {
                assertThat(serie.valor()).isEqualTo(valor);
                assertThat(serie.estimado()).isEqualTo(estimado);
            });
        });
    }

    private void entoncesElProgresoEstaVacio() {
        assertThat(progreso.hito()).isNull();
        assertThat(progreso.semanas()).isEmpty();
        assertThat(progreso.minis()).isEmpty();
    }
}