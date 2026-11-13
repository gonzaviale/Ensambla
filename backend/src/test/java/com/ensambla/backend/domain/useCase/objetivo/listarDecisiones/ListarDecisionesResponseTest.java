package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ListarDecisionesResponseTest {

    private static final LocalDate ESTA_SEMANA = LocalDate.of(2026, 11, 9);
    private static final LocalDate SEMANA_ANTERIOR = LocalDate.of(2026, 11, 2);

    @Test
    @DisplayName("Agrupa por semana, de la más nueva a la más vieja, y dentro de la semana de la más nueva a la más vieja")
    void deShouldGroupByWeekNewestFirst() {
        DecisionObjetivo vieja = decision("vieja", SEMANA_ANTERIOR, AHORA.minusDays(7));
        DecisionObjetivo primera = decision("primera", ESTA_SEMANA, AHORA.minusDays(1));
        DecisionObjetivo ultima = decision("ultima", ESTA_SEMANA, AHORA);

        ListarDecisionesResponse result = ListarDecisionesResponse.de(List.of(vieja, primera, ultima), Map.of());

        assertThat(result.semanas()).extracting(ListarDecisionesResponse.SemanaDecisiones::semanaInicio)
                .containsExactly(ESTA_SEMANA, SEMANA_ANTERIOR);
        assertThat(result.semanas().get(0).decisiones()).extracting(DecisionItem::descripcion)
                .containsExactly("ultima", "primera");
        assertThat(result.semanas().get(1).decisiones()).extracting(DecisionItem::descripcion)
                .containsExactly("vieja");
    }

    @Test
    @DisplayName("Copia todos los datos de la decisión")
    void deShouldCopyEveryField() {
        AvisoId avisoId = AvisoId.de("e0000000-0000-4000-8000-000000000001");
        DecisionObjetivo decision = new DecisionObjetivo(
                DecisionObjetivoId.de("00000000-0000-4000-8000-0000000000d1"), NEGOCIO_ID, OBJETIVO_ID, MINI_ID,
                TipoDecision.AVISO_DESCARTADO, avisoId, PRODUCTO_ID, "Bajá el precio", AHORA, ESTA_SEMANA);

        DecisionItem result = ListarDecisionesResponse.de(List.of(decision), Map.of())
                .semanas().getFirst().decisiones().getFirst();

        assertThat(result.id()).isEqualTo(decision.id());
        assertThat(result.tipo()).isEqualTo(TipoDecision.AVISO_DESCARTADO);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.avisoId()).isEqualTo(avisoId);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.descripcion()).isEqualTo("Bajá el precio");
        assertThat(result.tomadaEn()).isEqualTo(AHORA);
        assertThat(result.semanaInicio()).isEqualTo(ESTA_SEMANA);
    }

    // --- helpers ---
    private static DecisionObjetivo decision(String descripcion, LocalDate semana, OffsetDateTime tomadaEn) {
        return new DecisionObjetivo(DecisionObjetivoId.nuevo(), NEGOCIO_ID, OBJETIVO_ID, null,
                TipoDecision.PASO_ACEPTADO, null, null, descripcion, tomadaEn, semana);
    }
}
