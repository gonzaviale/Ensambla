package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlanJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlanRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.planEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanRepositoryImplTest {

    private final IPlanJpaRepository planJpaRepository = mock(IPlanJpaRepository.class);
    private final PlanRepositoryImpl planRepository = new PlanRepositoryImpl(
            planJpaRepository, new PlanRepositoryMapper());

    private Optional<Plan> encontrado;

    @Test
    @DisplayName("Recupera y mapea plan desde persistencia")
    void recuperaYMapeaElPlanDesdePersistencia() {
        dadoQueExisteElPlanGratuito();

        cuandoSeBuscaElPlanGratuito();

        entoncesSeEncuentraElPlanConTodosSusCampos();
    }

    @Test
    @DisplayName("Conserva el resultado vacío cuando no existe plan")
    void conservaElResultadoVacioCuandoNoExistePlan() {
        dadoQueNoExisteElPlanGratuito();

        cuandoSeBuscaElPlanGratuito();

        entoncesNoSeEncuentraNingunPlan();
    }

    // --- dado ---
    private void dadoQueExisteElPlanGratuito() {
        when(planJpaRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(planEntity()));
    }

    private void dadoQueNoExisteElPlanGratuito() {
        when(planJpaRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeBuscaElPlanGratuito() {
        encontrado = planRepository.findByCodigo(CodigoPlan.GRATUITO);
    }

    // --- entonces ---
    private void entoncesSeEncuentraElPlanConTodosSusCampos() {
        assertThat(encontrado).isPresent();
        Plan result = encontrado.orElseThrow();
        Plan esperado = plan();
        assertThat(result.getId()).isEqualTo(esperado.getId());
        assertThat(result.getCodigo()).isEqualTo(esperado.getCodigo());
        assertThat(result.getNombre()).isEqualTo(esperado.getNombre());
        assertThat(result.getConfigurado()).isEqualTo(esperado.getConfigurado());
        assertThat(result.getLimitesConfigurados()).isEqualTo(esperado.getLimitesConfigurados());
        assertThat(result.getPrecioMensual()).isEqualTo(esperado.getPrecioMensual());
        assertThat(result.getMoneda()).isEqualTo(esperado.getMoneda());
        assertThat(result.getLimiteProyectos()).isEqualTo(esperado.getLimiteProyectos());
        assertThat(result.getLimiteRegistros()).isEqualTo(esperado.getLimiteRegistros());
        assertThat(result.getLimitePaginasIaMensuales()).isEqualTo(esperado.getLimitePaginasIaMensuales());
        assertThat(result.getFunciones()).isEqualTo(esperado.getFunciones());
        assertThat(result.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
        verify(planJpaRepository).findByCodigo(CodigoPlan.GRATUITO);
    }

    private void entoncesNoSeEncuentraNingunPlan() {
        assertThat(encontrado).isEmpty();
    }
}
