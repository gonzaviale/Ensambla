package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlanJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISuscripcionJpaRepository;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanesPostgresIntegrationTest extends PostgresDatosTestSupport {

    @Autowired
    private IPlanJpaRepository planes;

    @Autowired
    private ISuscripcionJpaRepository suscripciones;

    @Autowired
    private SuscripcionService suscripcionService;

    @Autowired
    private RegistroService registroService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Carga las cuotas y el precio Premium aprobados para la renovación")
    void findByCodigoShouldReadApprovedCatalog() {
        PlanEntity gratuito = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow();

        PlanEntity premium = planes.findByCodigo(CodigoPlan.PREMIUM).orElseThrow();

        assertThat(gratuito.getNombre()).isEqualTo("Gratuito");
        assertThat(gratuito.getPrecioMensual()).isEqualByComparingTo(BigDecimal.ZERO);
        thenCommercialValuesPending(gratuito);
        assertThat(premium.getNombre()).isEqualTo("Premium");
        assertThat(premium.getPrecioMensual()).isEqualByComparingTo("14900.00");
        assertThat(premium.getMoneda()).isEqualTo("ARS");
        assertThat(premium.getConfigurado()).isTrue();
        assertThat(gratuito.getLimitesConfigurados()).isTrue();
        assertThat(gratuito.getLimiteProyectos()).isEqualTo(1);
        assertThat(gratuito.getLimiteRegistros()).isEqualTo(1000L);
        assertThat(gratuito.getLimitePaginasIaMensuales()).isEqualTo(10);
        assertThat(gratuito.getFunciones()).isEmpty();
        assertThat(premium.getLimitesConfigurados()).isTrue();
        assertThat(premium.getLimiteProyectos()).isNull();
        assertThat(premium.getLimiteRegistros()).isNull();
        assertThat(premium.getLimitePaginasIaMensuales()).isEqualTo(100);
        assertThat(premium.getFunciones()).containsExactly("RECETAS_COSTOS", "REPORTES_FILTRADOS", "EXPORTAR_EXCEL", "AVISOS_COMPLETOS", "ZONA_COMPLETA");
    }

    @Test
    @DisplayName("El registro asigna el plan gratuito y conserva las fechas en PostgreSQL")
    void registrarShouldPersistFreeSubscriptionWithAccount() {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                "Persona", "Nuevo negocio", "planes-registro@example.com", "ClaveSegura123!");

        Usuario result = registroService.registrar(request, AHORA);

        UUID negocioId = negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(result.getId()).orElseThrow().getId();
        entityManager.clear();
        Suscripcion suscripcion = suscripcionService.obtener(negocioId);
        assertThat(suscripcion.getId()).isNotNull();
        assertThat(suscripcion.getNegocioId()).isEqualTo(negocioId);
        assertThat(suscripcion.getPlanId()).isEqualTo(planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId());
        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(suscripcion.getInicioEn()).isEqualTo(AHORA);
        assertThat(suscripcion.getFinEn()).isNull();
        assertThat(suscripcion.getCreadoEn()).isEqualTo(AHORA);
        assertThat(suscripcion.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Buscar por negocio no devuelve la suscripción de otro negocio")
    void findByNegocioIdShouldIsolateSubscriptions() {
        UUID negocioA = givenNegocio("planes-a@example.com").getId();
        UUID negocioB = givenNegocio("planes-b@example.com").getId();
        suscripcionService.asignarGratuito(negocioA, AHORA);
        entityManager.clear();

        Optional<SuscripcionEntity> result = suscripciones.findByNegocioId(negocioB);

        assertThat(result).isEmpty();
        assertThat(suscripciones.findByNegocioId(negocioA)).isPresent();
    }

    @Test
    @DisplayName("La base rechaza una segunda suscripción actual para el mismo negocio")
    void saveShouldRejectDuplicateSubscription() {
        UUID negocioId = givenNegocio("planes-duplicado@example.com").getId();
        suscripcionService.asignarGratuito(negocioId, AHORA);

        assertThatThrownBy(() -> suscripcionService.asignarGratuito(negocioId, AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Borrar un negocio elimina su suscripción sin borrar el catálogo")
    void deleteNegocioShouldCascadeSubscriptionOnly() {
        UUID negocioId = givenNegocio("planes-borrar@example.com").getId();
        UUID suscripcionId = suscripcionService.asignarGratuito(negocioId, AHORA).getId();

        negocios.deleteById(negocioId);
        negocios.flush();
        entityManager.clear();

        assertThat(suscripciones.findById(suscripcionId)).isEmpty();
        assertThat(planes.findByCodigo(CodigoPlan.GRATUITO)).isPresent();
    }

    @Test
    @DisplayName("Guarda y relee límites, moneda, funciones y precio cuando se define el catálogo")
    void savePlanShouldPersistCommercialConfiguration() {
        PlanEntity gratuito = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow();
        gratuito.setConfigurado(true);
        gratuito.setMoneda("ARS");
        gratuito.setLimiteProyectos(3);
        gratuito.setLimiteRegistros(100L);
        gratuito.setLimitePaginasIaMensuales(10);
        gratuito.setFunciones(List.of("FUNCION_DE_PRUEBA"));

        planes.saveAndFlush(gratuito);
        entityManager.clear();

        PlanEntity result = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow();
        assertThat(result.getConfigurado()).isTrue();
        assertThat(result.getMoneda()).isEqualTo("ARS");
        assertThat(result.getLimiteProyectos()).isEqualTo(3);
        assertThat(result.getLimiteRegistros()).isEqualTo(100L);
        assertThat(result.getLimitePaginasIaMensuales()).isEqualTo(10);
        assertThat(result.getFunciones()).containsExactly("FUNCION_DE_PRUEBA");
        assertThat(result.getPrecioMensual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "limite_proyectos = -1",
            "limite_registros = -1",
            "limite_paginas_ia_mensuales = -1",
            "precio_mensual = -1",
            "moneda = 'pesos'",
            "funciones = '{}'::jsonb",
            "codigo = 'DESCONOCIDO'",
            "configurado = true",
            "precio_mensual = NULL"
    })
    @DisplayName("La base rechaza configuraciones de plan inválidas o incompletas")
    void updatePlanShouldRejectInvalidConfiguration(String asignacion) {
        UUID planId = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE plan SET " + asignacion + " WHERE id = ?", planId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un período que termina antes de empezar")
    void saveSubscriptionShouldRejectInvalidPeriod() {
        UUID negocioId = givenNegocio("planes-periodo@example.com").getId();
        UUID planId = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();

        assertThatThrownBy(() -> suscripciones.saveAndFlush(SuscripcionEntity.builder()
                .negocioId(negocioId)
                .planId(planId)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA)
                .finEn(AHORA.minusSeconds(1))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza una suscripción para un negocio inexistente")
    void saveSubscriptionShouldRejectMissingNegocio() {
        UUID planId = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();

        assertThatThrownBy(() -> suscripciones.saveAndFlush(SuscripcionEntity.builder()
                .negocioId(UUID.fromString("bb000000-0000-4000-8000-000000000099"))
                .planId(planId)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build())).isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- assert ---
    private void thenCommercialValuesPending(PlanEntity plan) {
        assertThat(plan.getConfigurado()).isFalse();
        assertThat(plan.getMoneda()).isNull();
    }
}
