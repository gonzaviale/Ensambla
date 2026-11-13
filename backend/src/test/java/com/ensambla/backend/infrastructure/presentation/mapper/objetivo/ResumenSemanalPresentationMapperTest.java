package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.DecisionesItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.MiniItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ReajusteItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.SemanaItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse.SemanaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenSemanalPresentationMapperTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private final ResumenSemanalPresentationMapper mapper = new ResumenSemanalPresentationMapper();

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión y el objetivo")
    void toRequestShouldCarryTheUserAndTheObjetivo() {
        ListarSemanasRequest result = mapper.toRequest(USUARIO_ID, OBJETIVO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Copia cada semana con sus decisiones y reajustes; un reajuste sin nuevo queda sin «a»")
    void toResponseShouldCopyEveryWeek() {
        SemanasObjetivoResponse result = mapper.toResponse(new ListarSemanasResponse(SEMANA, List.of(new SemanaItem(
                SEMANA, BigDecimal.TEN, BigDecimal.ONE, new DecisionesItem(1, 2, 3, 4), "Bien", true, List.of(
                        new ReajusteItem(TipoReajuste.REEMPLAZAR, "Sin avance", new MiniItem("Viejo", BigDecimal.ONE),
                                null))))));

        assertThat(result.ultimoCierre()).isEqualTo(SEMANA);
        SemanaResponse semana = result.semanas().get(0);
        assertThat(semana.semana()).isEqualTo(SEMANA);
        assertThat(semana.resultado()).isEqualTo(BigDecimal.TEN);
        assertThat(semana.avance()).isEqualTo(BigDecimal.ONE);
        assertThat(semana.decisiones().aplicadas()).isEqualTo(1);
        assertThat(semana.decisiones().descartadas()).isEqualTo(2);
        assertThat(semana.decisiones().aceptadas()).isEqualTo(3);
        assertThat(semana.decisiones().ignoradas()).isEqualTo(4);
        assertThat(semana.queFunciono()).isEqualTo("Bien");
        assertThat(semana.textoConIa()).isTrue();
        assertThat(semana.reajustes()).singleElement().satisfies(reajuste -> {
            assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
            assertThat(reajuste.motivo()).isEqualTo("Sin avance");
            assertThat(reajuste.de().titulo()).isEqualTo("Viejo");
            assertThat(reajuste.de().meta()).isEqualTo(BigDecimal.ONE);
            assertThat(reajuste.a()).isNull();
        });
    }

    @Test
    @DisplayName("Sin semanas devuelve una lista vacía")
    void toResponseShouldReturnAnEmptyListWhenThereAreNoWeeks() {
        SemanasObjetivoResponse result = mapper.toResponse(new ListarSemanasResponse(null, List.of()));

        assertThat(result.ultimoCierre()).isNull();
        assertThat(result.semanas()).isEmpty();
    }
}
