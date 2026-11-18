package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.planEntity;
import static org.assertj.core.api.Assertions.assertThat;

class PlanRepositoryMapperTest {

    private final PlanRepositoryMapper planRepositoryMapper = new PlanRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos de persistencia al dominio de plan")
    void unaEntidadSeLeeComoPlanConTodosSusDatos() {
        PlanEntity entidad = dadoUnaEntidadDePlan();

        Plan resultado = cuandoSeConvierteEnPlan(entidad);

        entoncesElPlanTieneTodosLosDatosDeLaEntidad(resultado);
    }

    @Test
    @DisplayName("Conserva los campos nulos en toDomain de plan")
    void unaEntidadVaciaSeLeeComoPlanConCamposNulos() {
        PlanEntity entidad = dadoUnaEntidadVacia();

        Plan resultado = cuandoSeConvierteEnPlan(entidad);

        entoncesElPlanTieneTodosLosCamposNulos(resultado);
    }

    // --- dado ---
    private PlanEntity dadoUnaEntidadDePlan() {
        return planEntity();
    }

    private PlanEntity dadoUnaEntidadVacia() {
        return new PlanEntity();
    }

    // --- cuando ---
    private Plan cuandoSeConvierteEnPlan(PlanEntity entidad) {
        return planRepositoryMapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesElPlanTieneTodosLosDatosDeLaEntidad(Plan resultado) {
        Plan esperado = plan();
        assertThat(resultado.getId()).isEqualTo(esperado.getId());
        assertThat(resultado.getCodigo()).isEqualTo(esperado.getCodigo());
        assertThat(resultado.getNombre()).isEqualTo(esperado.getNombre());
        assertThat(resultado.getConfigurado()).isEqualTo(esperado.getConfigurado());
        assertThat(resultado.getLimitesConfigurados()).isEqualTo(esperado.getLimitesConfigurados());
        assertThat(resultado.getPrecioMensual()).isEqualTo(esperado.getPrecioMensual());
        assertThat(resultado.getMoneda()).isEqualTo(esperado.getMoneda());
        assertThat(resultado.getLimiteProyectos()).isEqualTo(esperado.getLimiteProyectos());
        assertThat(resultado.getLimiteRegistros()).isEqualTo(esperado.getLimiteRegistros());
        assertThat(resultado.getLimitePaginasIaMensuales()).isEqualTo(esperado.getLimitePaginasIaMensuales());
        assertThat(resultado.getLimiteObjetivosIaMensuales()).isEqualTo(esperado.getLimiteObjetivosIaMensuales());
        assertThat(resultado.getFunciones()).isEqualTo(esperado.getFunciones());
        assertThat(resultado.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(resultado.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }

    private void entoncesElPlanTieneTodosLosCamposNulos(Plan resultado) {
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getCodigo()).isNull();
        assertThat(resultado.getNombre()).isNull();
        assertThat(resultado.getConfigurado()).isNull();
        assertThat(resultado.getPrecioMensual()).isNull();
        assertThat(resultado.getMoneda()).isNull();
        assertThat(resultado.getLimiteProyectos()).isNull();
        assertThat(resultado.getLimiteRegistros()).isNull();
        assertThat(resultado.getLimitePaginasIaMensuales()).isNull();
        assertThat(resultado.getLimiteObjetivosIaMensuales()).isNull();
        assertThat(resultado.getFunciones()).isNull();
        assertThat(resultado.getCreadoEn()).isNull();
        assertThat(resultado.getActualizadoEn()).isNull();
    }
}
