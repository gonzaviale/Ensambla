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
    private PlanEntity origen;
    private Plan result;

    @Test
    @DisplayName("Mapea todos los campos de persistencia al dominio de plan")
    void toDomainShouldMapEveryField() {
        origen = planEntity();

        whenSeLeeLaEntidad();

        thenToDomainMatches();
    }

    @Test
    @DisplayName("Conserva los campos nulos en toDomain de plan")
    void toDomainShouldPreserveNullFields() {
        givenUnaEntidadVacia();

        whenSeLeeLaEntidad();

        thenTodosLosCamposSonNulos();
    }

    // --- arrange ---
    private void givenUnaEntidadVacia() {
        origen = new PlanEntity();
    }

    // --- act ---
    private void whenSeLeeLaEntidad() {
        result = planRepositoryMapper.toDomain(origen);
    }

    // --- assert ---
    private void thenTodosLosCamposSonNulos() {
        assertThat(result.getId()).isNull();
        assertThat(result.getCodigo()).isNull();
        assertThat(result.getNombre()).isNull();
        assertThat(result.getConfigurado()).isNull();
        assertThat(result.getPrecioMensual()).isNull();
        assertThat(result.getMoneda()).isNull();
        assertThat(result.getLimiteProyectos()).isNull();
        assertThat(result.getLimiteRegistros()).isNull();
        assertThat(result.getLimitePaginasIaMensuales()).isNull();
        assertThat(result.getLimiteObjetivosIaMensuales()).isNull();
        assertThat(result.getFunciones()).isNull();
        assertThat(result.getCreadoEn()).isNull();
        assertThat(result.getActualizadoEn()).isNull();
    }

    private void thenToDomainMatches() {
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
        assertThat(result.getLimiteObjetivosIaMensuales()).isEqualTo(esperado.getLimiteObjetivosIaMensuales());
        assertThat(result.getFunciones()).isEqualTo(esperado.getFunciones());
        assertThat(result.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }
}
