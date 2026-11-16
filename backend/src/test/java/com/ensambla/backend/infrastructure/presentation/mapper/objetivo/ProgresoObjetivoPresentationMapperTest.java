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
    private ObtenerProgresoRequest pedido;
    private ProgresoObjetivoResponse respuesta;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión y el objetivo de la ruta")
    void toRequestShouldCarryTheUserAndTheObjetivo() {
        whenSeArmaElPedido();

        thenElPedidoTraeUsuarioYObjetivo();
    }

    @Test
    @DisplayName("Copia la meta, el hito, la serie del objetivo y la de cada mini-objetivo")
    void toResponseShouldCopyEveryField() {
        whenSeMapeaElProgreso(progresoCompleto());

        thenSeCopianLaMetaElHitoYLasSeries();
    }

    @Test
    @DisplayName("Sin semanas ni mini-objetivos devuelve listas vacías")
    void toResponseShouldReturnEmptyLists() {
        whenSeMapeaElProgreso(progresoVacio());

        thenNoHayHitoNiSeries();
    }

    // --- act ---
    private void whenSeArmaElPedido() {
        pedido = mapper.toRequest(USUARIO_ID, OBJETIVO_ID);
    }

    private void whenSeMapeaElProgreso(ObtenerProgresoResponse progreso) {
        respuesta = mapper.toResponse(progreso);
    }

    // --- assert ---
    private void thenElPedidoTraeUsuarioYObjetivo() {
        assertThat(pedido.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void thenSeCopianLaMetaElHitoYLasSeries() {
        assertThat(respuesta.meta()).isEqualTo(META);
        assertThat(respuesta.hito()).isEqualTo(HitoObjetivo.OBJETIVO_ALCANZADO);
        assertThat(respuesta.semanas()).singleElement().satisfies(item -> {
            assertThat(item.semana()).isEqualTo(SEMANA);
            assertThat(item.valor()).isEqualTo(new BigDecimal("-27000"));
            assertThat(item.estimado()).isTrue();
        });
        assertThat(respuesta.minis()).singleElement().satisfies(item -> {
            assertThat(item.mini().id()).isEqualTo(MINI_ID);
            assertThat(item.hito()).isEqualTo(HitoObjetivo.SIN_AVANCE);
            assertThat(item.semanas()).singleElement().satisfies(serie -> {
                assertThat(serie.valor()).isEqualTo(BigDecimal.TEN);
                assertThat(serie.estimado()).isFalse();
            });
        });
    }

    private void thenNoHayHitoNiSeries() {
        assertThat(respuesta.hito()).isNull();
        assertThat(respuesta.semanas()).isEmpty();
        assertThat(respuesta.minis()).isEmpty();
    }

    // --- helpers ---
    private static ObtenerProgresoResponse progresoCompleto() {
        PuntoSemanal punto = new PuntoSemanal(SEMANA, new BigDecimal("-27000"), true);
        MiniProgreso mini = new MiniProgreso(MiniObjetivoItem.de(miniObjetivo()), HitoObjetivo.SIN_AVANCE,
                List.of(new PuntoSemanal(SEMANA, BigDecimal.TEN, false)));
        return new ObtenerProgresoResponse(META, HitoObjetivo.OBJETIVO_ALCANZADO, List.of(punto), List.of(mini));
    }

    private static ObtenerProgresoResponse progresoVacio() {
        return new ObtenerProgresoResponse(META, null, List.of(), List.of());
    }
}
