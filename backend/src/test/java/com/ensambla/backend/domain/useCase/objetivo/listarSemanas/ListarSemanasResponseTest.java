package com.ensambla.backend.domain.useCase.objetivo.listarSemanas;

import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.SemanaItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ListarSemanasResponseTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    @Test
    @DisplayName("El último cierre es la semana del primer resumen y se copia cada campo")
    void deShouldCopyEverySummaryAndTakeTheNewestAsLastClose() {
        ListarSemanasResponse result = ListarSemanasResponse.de(List.of(
                resumen(SEMANA, List.of(reajuste("Nuevo", new BigDecimal("110")))),
                resumen(SEMANA.minusWeeks(1), List.of())));

        assertThat(result.ultimoCierre()).isEqualTo(SEMANA);
        assertThat(result.semanas()).extracting(SemanaItem::semana).containsExactly(SEMANA, SEMANA.minusWeeks(1));
        SemanaItem semana = result.semanas().get(0);
        assertThat(semana.resultado()).isEqualByComparingTo("180000");
        assertThat(semana.avance()).isEqualByComparingTo("0.45");
        assertThat(semana.decisiones().aplicadas()).isEqualTo(1);
        assertThat(semana.decisiones().descartadas()).isEqualTo(2);
        assertThat(semana.decisiones().aceptadas()).isEqualTo(3);
        assertThat(semana.decisiones().ignoradas()).isEqualTo(4);
        assertThat(semana.queFunciono()).isEqualTo("Bien");
        assertThat(semana.textoConIa()).isFalse();
        assertThat(semana.reajustes()).singleElement().satisfies(item -> {
            assertThat(item.tipo()).isEqualTo(TipoReajuste.SUBIR_META);
            assertThat(item.motivo()).isEqualTo("Lo lograste");
            assertThat(item.de().titulo()).isEqualTo("Viejo");
            assertThat(item.de().meta()).isEqualByComparingTo("100");
            assertThat(item.a().titulo()).isEqualTo("Nuevo");
            assertThat(item.a().meta()).isEqualByComparingTo("110");
        });
    }

    @Test
    @DisplayName("Un reajuste cuyo mini-objetivo nuevo ya no existe queda sin «a»")
    void deShouldLeaveTheFollowerEmptyWhenItNoLongerExists() {
        ListarSemanasResponse result = ListarSemanasResponse.de(List.of(resumen(SEMANA, List.of(reajuste(null, null)))));

        assertThat(result.semanas().get(0).reajustes().get(0).a()).isNull();
    }

    @Test
    @DisplayName("Sin resúmenes no hay último cierre")
    void deShouldHaveNoLastCloseWhenThereAreNoSummaries() {
        ListarSemanasResponse result = ListarSemanasResponse.de(List.of());

        assertThat(result.ultimoCierre()).isNull();
        assertThat(result.semanas()).isEmpty();
    }

    // --- helpers ---
    private static ResumenSemanal resumen(LocalDate semana, List<ReajusteMiniObjetivo> reajustes) {
        return new ResumenSemanal(ResumenSemanalId.nuevo(), OBJETIVO_ID, semana, new BigDecimal("180000"),
                new BigDecimal("0.45"), new ConteoDecisiones(1, 2, 3, 4), List.of(new CierreMini(MINI_ID,
                BigDecimal.ONE)), "Bien", false, AHORA, reajustes);
    }

    private static ReajusteMiniObjetivo reajuste(String tituloNuevo, BigDecimal metaNueva) {
        return new ReajusteMiniObjetivo(ReajusteMiniObjetivoId.nuevo(), MINI_ID, OTRO_MINI_ID,
                TipoReajuste.SUBIR_META, "Lo lograste", "Viejo", new BigDecimal("100"), tituloNuevo, metaNueva, AHORA);
    }
}
