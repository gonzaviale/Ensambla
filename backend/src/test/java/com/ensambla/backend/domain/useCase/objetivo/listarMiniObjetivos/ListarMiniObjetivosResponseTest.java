package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ListarMiniObjetivosResponseTest {

    @Test
    @DisplayName("Separa los que están en curso, por posición, de la historia, del más nuevo al más viejo")
    void deShouldSplitInProgressFromHistory() {
        MiniObjetivo segundo = mini(2, EstadoMiniObjetivo.EN_CURSO, AHORA);
        MiniObjetivo primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        MiniObjetivo reemplazado = mini(3, EstadoMiniObjetivo.REEMPLAZADO, AHORA.minusDays(2));
        MiniObjetivo logrado = mini(3, EstadoMiniObjetivo.LOGRADO, AHORA.minusDays(1));
        logrado.setReemplazadoPor(MiniObjetivoId.nuevo());

        ListarMiniObjetivosResponse result = ListarMiniObjetivosResponse.de(
                objetivo(), List.of(segundo, reemplazado, primero, logrado));

        assertThat(result.minis()).extracting(MiniObjetivoItem::id).containsExactly(primero.getId(), segundo.getId());
        assertThat(result.historial()).extracting(MiniObjetivoItem::id)
                .containsExactly(logrado.getId(), reemplazado.getId());
    }

    @Test
    @DisplayName("Un logrado sigue en su lugar hasta el cierre del domingo y cuenta como ocupado")
    void deShouldKeepAnAchievedMiniUntilTheWeeklyClose() {
        MiniObjetivo primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        MiniObjetivo logrado = mini(2, EstadoMiniObjetivo.LOGRADO, AHORA.minusDays(1));
        MiniObjetivo tercero = mini(3, EstadoMiniObjetivo.EN_CURSO, AHORA);

        ListarMiniObjetivosResponse result = ListarMiniObjetivosResponse.de(
                objetivo(), List.of(tercero, logrado, primero));

        assertThat(result.minis()).extracting(MiniObjetivoItem::id)
                .containsExactly(primero.getId(), logrado.getId(), tercero.getId());
        assertThat(result.historial()).isEmpty();
        assertThat(result.faltanDatos()).isNull();
    }

    @Test
    @DisplayName("Un logrado cuyo lugar ya ocupa otro en curso queda en la historia")
    void deShouldMoveAnAchievedMiniToHistoryWhenItsPlaceIsTaken() {
        MiniObjetivo logrado = mini(1, EstadoMiniObjetivo.LOGRADO, AHORA.minusDays(1));
        MiniObjetivo nuevo = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        ListarMiniObjetivosResponse result = ListarMiniObjetivosResponse.de(objetivo(), List.of(logrado, nuevo));

        assertThat(result.minis()).extracting(MiniObjetivoItem::id).containsExactly(nuevo.getId());
        assertThat(result.historial()).extracting(MiniObjetivoItem::id).containsExactly(logrado.getId());
    }

    @Test
    @DisplayName("Con los 3 en curso no falta ningún dato")
    void deShouldNotReportMissingDataWhenThereAreThreeInProgress() {
        List<MiniObjetivo> tres = List.of(
                mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA),
                mini(2, EstadoMiniObjetivo.EN_CURSO, AHORA),
                mini(3, EstadoMiniObjetivo.EN_CURSO, AHORA));

        ListarMiniObjetivosResponse result = ListarMiniObjetivosResponse.de(objetivo(), tres);

        assertThat(result.faltanDatos()).isNull();
    }

    @Test
    @DisplayName("Con menos de 3 en curso avisa que faltan datos para proponer más")
    void deShouldReportMissingDataWhenThereAreFewerThanThree() {
        ListarMiniObjetivosResponse result = ListarMiniObjetivosResponse.de(
                objetivo(), List.of(mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA)));

        assertThat(result.faltanDatos()).isEqualTo(FaltanDatosMiniObjetivos.POCOS_DATOS);
    }

    @Test
    @DisplayName("Un objetivo alcanzado lo dice, tenga o no mini-objetivos")
    void deShouldReportAReachedObjetivo() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);

        ListarMiniObjetivosResponse result = ListarMiniObjetivosResponse.de(alcanzado, List.of());

        assertThat(result.faltanDatos()).isEqualTo(FaltanDatosMiniObjetivos.OBJETIVO_ALCANZADO);
        assertThat(result.minis()).isEmpty();
        assertThat(result.historial()).isEmpty();
    }

    // --- helpers ---
    private static MiniObjetivo mini(int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        MiniObjetivo mini = miniObjetivo();
        mini.setId(MiniObjetivoId.nuevo());
        mini.setPosicion(posicion);
        mini.setEstado(estado);
        mini.setCreadoEn(creadoEn);
        return mini;
    }
}
