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
    void unaEntidadSeLeeComoSuscripcionConTodosSusDatos() {
        SuscripcionEntity entidad = dadoUnaEntidadDeSuscripcion();

        Suscripcion resultado = cuandoSeConvierteEnSuscripcion(entidad);

        entoncesLaSuscripcionTieneTodosLosDatosDeLaEntidad(resultado);
    }

    @Test
    @DisplayName("Conserva los campos nulos en toDomain de suscripcion")
    void unaEntidadVaciaSeLeeComoSuscripcionConCamposNulos() {
        SuscripcionEntity entidad = dadoUnaEntidadVacia();

        Suscripcion resultado = cuandoSeConvierteEnSuscripcion(entidad);

        entoncesLaSuscripcionTieneTodosLosCamposNulos(resultado);
    }

    @Test
    @DisplayName("Mapea todos los campos del dominio a persistencia de suscripcion")
    void unaSuscripcionSeGuardaConTodosSusDatos() {
        Suscripcion origen = dadoUnaSuscripcion();

        SuscripcionEntity resultado = cuandoSeConvierteEnEntidad(origen);

        entoncesLaEntidadTieneTodosLosDatosDeLaSuscripcion(resultado);
    }

    @Test
    @DisplayName("Conserva los campos nulos en toEntity de suscripcion")
    void unaSuscripcionVaciaSeGuardaConCamposNulos() {
        Suscripcion origen = dadoUnaSuscripcionVacia();

        SuscripcionEntity resultado = cuandoSeConvierteEnEntidad(origen);

        entoncesLaEntidadTieneTodosLosCamposNulos(resultado);
    }

    // --- dado ---
    private SuscripcionEntity dadoUnaEntidadDeSuscripcion() {
        return suscripcionEntity();
    }

    private SuscripcionEntity dadoUnaEntidadVacia() {
        return new SuscripcionEntity();
    }

    private Suscripcion dadoUnaSuscripcion() {
        return suscripcion();
    }

    private Suscripcion dadoUnaSuscripcionVacia() {
        return new Suscripcion();
    }

    // --- cuando ---
    private Suscripcion cuandoSeConvierteEnSuscripcion(SuscripcionEntity entidad) {
        return suscripcionRepositoryMapper.toDomain(entidad);
    }

    private SuscripcionEntity cuandoSeConvierteEnEntidad(Suscripcion suscripcion) {
        return suscripcionRepositoryMapper.toEntity(suscripcion);
    }

    // --- entonces ---
    private void entoncesLaSuscripcionTieneTodosLosDatosDeLaEntidad(Suscripcion resultado) {
        Suscripcion esperado = suscripcion();
        assertThat(resultado.getId()).isEqualTo(esperado.getId());
        assertThat(resultado.getNegocioId()).isEqualTo(esperado.getNegocioId());
        assertThat(resultado.getPlanId()).isEqualTo(esperado.getPlanId());
        assertThat(resultado.getEstado()).isEqualTo(esperado.getEstado());
        assertThat(resultado.getInicioEn()).isEqualTo(esperado.getInicioEn());
        assertThat(resultado.getFinEn()).isEqualTo(esperado.getFinEn());
        assertThat(resultado.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(resultado.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }

    private void entoncesLaSuscripcionTieneTodosLosCamposNulos(Suscripcion resultado) {
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getNegocioId()).isNull();
        assertThat(resultado.getPlanId()).isNull();
        assertThat(resultado.getEstado()).isNull();
        assertThat(resultado.getInicioEn()).isNull();
        assertThat(resultado.getFinEn()).isNull();
        assertThat(resultado.getCreadoEn()).isNull();
        assertThat(resultado.getActualizadoEn()).isNull();
    }

    private void entoncesLaEntidadTieneTodosLosDatosDeLaSuscripcion(SuscripcionEntity resultado) {
        SuscripcionEntity esperado = suscripcionEntity();
        assertThat(resultado.getId()).isEqualTo(esperado.getId());
        assertThat(resultado.getNegocioId()).isEqualTo(esperado.getNegocioId());
        assertThat(resultado.getPlanId()).isEqualTo(esperado.getPlanId());
        assertThat(resultado.getEstado()).isEqualTo(esperado.getEstado());
        assertThat(resultado.getInicioEn()).isEqualTo(esperado.getInicioEn());
        assertThat(resultado.getFinEn()).isEqualTo(esperado.getFinEn());
        assertThat(resultado.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(resultado.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }

    private void entoncesLaEntidadTieneTodosLosCamposNulos(SuscripcionEntity resultado) {
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getNegocioId()).isNull();
        assertThat(resultado.getPlanId()).isNull();
        assertThat(resultado.getEstado()).isNull();
        assertThat(resultado.getInicioEn()).isNull();
        assertThat(resultado.getFinEn()).isNull();
        assertThat(resultado.getCreadoEn()).isNull();
        assertThat(resultado.getActualizadoEn()).isNull();
    }
}
