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

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión y el objetivo de la ruta")
    void toRequestShouldCarryTheUserAndTheObjetivo() {
        ObtenerProgresoRequest result = mapper.toRequest(USUARIO_ID, OBJETIVO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Copia la meta, el hito, la serie del objetivo y la de cada mini-objetivo")
    void toResponseShouldCopyEveryField() {
        PuntoSemanal punto = new PuntoSemanal(SEMANA, new BigDecimal("-27000"), true);
        MiniProgreso mini = new MiniProgreso(MiniObjetivoItem.de(miniObjetivo()), HitoObjetivo.SIN_AVANCE,
                List.of(new PuntoSemanal(SEMANA, BigDecimal.TEN, false)));

        ProgresoObjetivoResponse result = mapper.toResponse(
                new ObtenerProgresoResponse(META, HitoObjetivo.OBJETIVO_ALCANZADO, List.of(punto), List.of(mini)));

        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.hito()).isEqualTo(HitoObjetivo.OBJETIVO_ALCANZADO);
        assertThat(result.semanas()).singleElement().satisfies(item -> {
            assertThat(item.semana()).isEqualTo(SEMANA);
            assertThat(item.valor()).isEqualTo(new BigDecimal("-27000"));
            assertThat(item.estimado()).isTrue();
        });
        assertThat(result.minis()).singleElement().satisfies(item -> {
            assertThat(item.mini().id()).isEqualTo(MINI_ID);
            assertThat(item.hito()).isEqualTo(HitoObjetivo.SIN_AVANCE);
            assertThat(item.semanas()).singleElement().satisfies(serie -> {
                assertThat(serie.valor()).isEqualTo(BigDecimal.TEN);
                assertThat(serie.estimado()).isFalse();
            });
        });
    }

    @Test
    @DisplayName("Sin semanas ni mini-objetivos devuelve listas vacías")
    void toResponseShouldReturnEmptyLists() {
        ObtenerProgresoResponse vacia = new ObtenerProgresoResponse(META, null, List.of(), List.of());

        ProgresoObjetivoResponse result = mapper.toResponse(vacia);

        assertThat(result.hito()).isNull();
        assertThat(result.semanas()).isEmpty();
        assertThat(result.minis()).isEmpty();
    }
}
