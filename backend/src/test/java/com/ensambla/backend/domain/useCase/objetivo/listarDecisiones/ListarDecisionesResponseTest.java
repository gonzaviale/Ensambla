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
    private static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");

    private ListarDecisionesResponse respuesta;
    private DecisionObjetivo decision;

    @Test
    @DisplayName("Agrupa por semana, de la más nueva a la más vieja, y dentro de la semana de la más nueva a la más vieja")
    void agrupaPorSemanaDeLaMasNuevaALaMasViejaYDentroDeLaSemanaDeLaMasNuevaALaMasVieja() {
        cuandoSeArmaLaRespuestaDeTresDecisionesEnDosSemanas();

        entoncesLasSemanasVanDeLaMasNuevaALaMasVieja();
        entoncesLasDecisionesDeCadaSemanaVanDeLaMasNuevaALaMasVieja();
    }

    @Test
    @DisplayName("Copia todos los datos de la decisión")
    void copiaTodosLosDatosDeLaDecision() {
        cuandoSeArmaLaRespuestaDeUnDescarteDeAviso();

        entoncesCopiaTodosLosDatosDeLaDecision();
    }

    // --- cuando ---

    private void cuandoSeArmaLaRespuestaDeTresDecisionesEnDosSemanas() {
        DecisionObjetivo vieja = unaDecision("vieja", SEMANA_ANTERIOR, AHORA.minusDays(7));
        DecisionObjetivo primera = unaDecision("primera", ESTA_SEMANA, AHORA.minusDays(1));
        DecisionObjetivo ultima = unaDecision("ultima", ESTA_SEMANA, AHORA);
        respuesta = ListarDecisionesResponse.de(List.of(vieja, primera, ultima), Map.of());
    }

    private void cuandoSeArmaLaRespuestaDeUnDescarteDeAviso() {
        decision = new DecisionObjetivo(
                DecisionObjetivoId.de("00000000-0000-4000-8000-0000000000d1"), NEGOCIO_ID, OBJETIVO_ID, MINI_ID,
                TipoDecision.AVISO_DESCARTADO, AVISO_ID, PRODUCTO_ID, "Bajá el precio", AHORA, ESTA_SEMANA);
        respuesta = ListarDecisionesResponse.de(List.of(decision), Map.of());
    }

    // --- entonces ---

    private void entoncesLasSemanasVanDeLaMasNuevaALaMasVieja() {
        assertThat(respuesta.semanas()).extracting(ListarDecisionesResponse.SemanaDecisiones::semanaInicio)
                .containsExactly(ESTA_SEMANA, SEMANA_ANTERIOR);
    }

    private void entoncesLasDecisionesDeCadaSemanaVanDeLaMasNuevaALaMasVieja() {
        assertThat(respuesta.semanas().get(0).decisiones()).extracting(DecisionItem::descripcion)
                .containsExactly("ultima", "primera");
        assertThat(respuesta.semanas().get(1).decisiones()).extracting(DecisionItem::descripcion)
                .containsExactly("vieja");
    }

    private void entoncesCopiaTodosLosDatosDeLaDecision() {
        DecisionItem item = respuesta.semanas().getFirst().decisiones().getFirst();
        assertThat(item.id()).isEqualTo(decision.id());
        assertThat(item.tipo()).isEqualTo(TipoDecision.AVISO_DESCARTADO);
        assertThat(item.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(item.avisoId()).isEqualTo(AVISO_ID);
        assertThat(item.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(item.descripcion()).isEqualTo("Bajá el precio");
        assertThat(item.tomadaEn()).isEqualTo(AHORA);
        assertThat(item.semanaInicio()).isEqualTo(ESTA_SEMANA);
    }

    // --- datos ---

    private static DecisionObjetivo unaDecision(String descripcion, LocalDate semana, OffsetDateTime tomadaEn) {
        return new DecisionObjetivo(DecisionObjetivoId.nuevo(), NEGOCIO_ID, OBJETIVO_ID, null,
                TipoDecision.PASO_ACEPTADO, null, null, descripcion, tomadaEn, semana);
    }
}
