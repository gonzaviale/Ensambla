package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static com.ensambla.backend.support.PlanTestData.suscripcionEntity;
import static org.assertj.core.api.Assertions.assertThat;

class SuscripcionRepositoryMapperTest {

    private final SuscripcionRepositoryMapper suscripcionRepositoryMapper = new SuscripcionRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos de persistencia al dominio de suscripcion")
    void toDomainShouldMapEveryField() {
        SuscripcionEntity origen = suscripcionEntity();

        Suscripcion result = suscripcionRepositoryMapper.toDomain(origen);

        thenToDomainMatches(result);
    }

    @Test
    @DisplayName("Conserva los campos nulos en toDomain de suscripcion")
    void toDomainShouldPreserveNullFields() {
        SuscripcionEntity origen = new SuscripcionEntity();

        Suscripcion result = suscripcionRepositoryMapper.toDomain(origen);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isNull();
        assertThat(result.getPlanId()).isNull();
        assertThat(result.getEstado()).isNull();
        assertThat(result.getInicioEn()).isNull();
        assertThat(result.getFinEn()).isNull();
        assertThat(result.getCreadoEn()).isNull();
        assertThat(result.getActualizadoEn()).isNull();
    }

    @Test
    @DisplayName("Mapea todos los campos del dominio a persistencia de suscripcion")
    void toEntityShouldMapEveryField() {
        Suscripcion origen = suscripcion();

        SuscripcionEntity result = suscripcionRepositoryMapper.toEntity(origen);

        thenToEntityMatches(result);
    }

    @Test
    @DisplayName("Conserva los campos nulos en toEntity de suscripcion")
    void toEntityShouldPreserveNullFields() {
        Suscripcion origen = new Suscripcion();

        SuscripcionEntity result = suscripcionRepositoryMapper.toEntity(origen);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isNull();
        assertThat(result.getPlanId()).isNull();
        assertThat(result.getEstado()).isNull();
        assertThat(result.getInicioEn()).isNull();
        assertThat(result.getFinEn()).isNull();
        assertThat(result.getCreadoEn()).isNull();
        assertThat(result.getActualizadoEn()).isNull();
    }

    // --- assert ---
    private void thenToDomainMatches(Suscripcion result) {
        Suscripcion esperado = suscripcion();
        assertThat(result.getId()).isEqualTo(esperado.getId());
        assertThat(result.getNegocioId()).isEqualTo(esperado.getNegocioId());
        assertThat(result.getPlanId()).isEqualTo(esperado.getPlanId());
        assertThat(result.getEstado()).isEqualTo(esperado.getEstado());
        assertThat(result.getInicioEn()).isEqualTo(esperado.getInicioEn());
        assertThat(result.getFinEn()).isEqualTo(esperado.getFinEn());
        assertThat(result.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }

    private void thenToEntityMatches(SuscripcionEntity result) {
        SuscripcionEntity esperado = suscripcionEntity();
        assertThat(result.getId()).isEqualTo(esperado.getId());
        assertThat(result.getNegocioId()).isEqualTo(esperado.getNegocioId());
        assertThat(result.getPlanId()).isEqualTo(esperado.getPlanId());
        assertThat(result.getEstado()).isEqualTo(esperado.getEstado());
        assertThat(result.getInicioEn()).isEqualTo(esperado.getInicioEn());
        assertThat(result.getFinEn()).isEqualTo(esperado.getFinEn());
        assertThat(result.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }
}
