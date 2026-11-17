package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuario;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuarioRequest;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlanJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISuscripcionJpaRepository;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
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
import static org.assertj.core.api.Assertions.catchThrowable;

class PlanesPostgresIntegrationTest extends PostgresDatosTestSupport {

    private static final UUID NEGOCIO_INEXISTENTE = UUID.fromString("bb000000-0000-4000-8000-000000000099");

    @Autowired
    private IPlanJpaRepository planes;

    @Autowired
    private ISuscripcionJpaRepository suscripciones;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private RegistrarUsuario registrarUsuario;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PlanEntity gratuito;
    private PlanEntity premium;
    private PlanEntity planLeido;
    private UsuarioResponse usuarioRegistrado;
    private Suscripcion suscripcionLeida;
    private Optional<SuscripcionEntity> suscripcionBuscada;
    private UUID negocioA;
    private UUID negocioB;
    private UUID suscripcionId;
    private Throwable error;

    @Test
    @DisplayName("Carga las cuotas y el precio Premium aprobados para la renovación")
    void findByCodigoShouldReadApprovedCatalog() {
        whenSeLeeElCatalogo();

        thenElGratuitoTieneSusCuotasAprobadas();
        thenElPremiumTieneSuPrecioYSusCuotasAprobadas();
    }

    @Test
    @DisplayName("El registro asigna el plan gratuito y conserva las fechas en PostgreSQL")
    void registrarShouldPersistFreeSubscriptionWithAccount() {
        whenSeRegistraUnUsuario();

        thenSeLeeLaSuscripcionGratuitaConSusFechas();
    }

    @Test
    @DisplayName("Buscar por negocio no devuelve la suscripción de otro negocio")
    void findByNegocioIdShouldIsolateSubscriptions() {
        givenUnaSuscripcionGratuitaParaA();

        whenSeBuscaLaSuscripcionDeB();

        thenNoSeEncuentraLaDeB();
        thenSigueLaDeA();
    }

    @Test
    @DisplayName("La base rechaza una segunda suscripción actual para el mismo negocio")
    void saveShouldRejectDuplicateSubscription() {
        givenUnaSuscripcionGratuitaParaA();

        whenSeAsignaOtraVezElGratuitoAA();

        thenLaBaseLaRechaza();
    }

    @Test
    @DisplayName("Borrar un negocio elimina su suscripción sin borrar el catálogo")
    void deleteNegocioShouldCascadeSubscriptionOnly() {
        givenUnaSuscripcionGratuitaParaA();

        whenSeBorraElNegocioA();

        thenSeBorraLaSuscripcionPeroNoElCatalogo();
    }

    @Test
    @DisplayName("Guarda y relee límites, moneda, funciones y precio cuando se define el catálogo")
    void savePlanShouldPersistCommercialConfiguration() {
        whenSeDefineLaConfiguracionComercialDelGratuito();

        thenSeRelenLimitesMonedaFuncionesYPrecio();
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
        whenSeActualizaElGratuitoCon(asignacion);

        thenLaBaseLaRechaza();
    }

    @Test
    @DisplayName("La base rechaza un período que termina antes de empezar")
    void saveSubscriptionShouldRejectInvalidPeriod() {
        givenUnNegocioA();

        whenSeGuardaUnaSuscripcionQueTerminaAntesDeEmpezar();

        thenLaBaseLaRechaza();
    }

    @Test
    @DisplayName("La base rechaza una suscripción para un negocio inexistente")
    void saveSubscriptionShouldRejectMissingNegocio() {
        whenSeGuardaUnaSuscripcionParaUnNegocioInexistente();

        thenLaBaseLaRechaza();
    }

    // --- arrange ---
    private void givenUnNegocioA() {
        negocioA = givenNegocio("planes-periodo@example.com").getId();
    }

    private void givenUnaSuscripcionGratuitaParaA() {
        negocioA = givenNegocio("planes-a@example.com").getId();
        negocioB = givenNegocio("planes-b@example.com").getId();
        suscripcionId = suscripcionService.asignarGratuito(NegocioId.de(negocioA), AHORA).getId().valor();
        entityManager.clear();
    }

    // --- act ---
    private void whenSeLeeElCatalogo() {
        gratuito = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow();
        premium = planes.findByCodigo(CodigoPlan.PREMIUM).orElseThrow();
    }

    private void whenSeRegistraUnUsuario() {
        usuarioRegistrado = registrarUsuario.execute(new RegistrarUsuarioRequest(
                "Persona", "Nuevo negocio", "planes-registro@example.com", "ClaveSegura123!"));
        UUID negocioId = negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioRegistrado.id().valor())
                .orElseThrow().getId();
        entityManager.clear();
        suscripcionLeida = suscripcionRepository.findByNegocioId(NegocioId.de(negocioId)).orElseThrow();
        negocioA = negocioId;
    }

    private void whenSeBuscaLaSuscripcionDeB() {
        suscripcionBuscada = suscripciones.findByNegocioId(negocioB);
    }

    private void whenSeAsignaOtraVezElGratuitoAA() {
        error = catchThrowable(() -> suscripcionService.asignarGratuito(NegocioId.de(negocioA), AHORA));
    }

    private void whenSeBorraElNegocioA() {
        negocios.deleteById(negocioA);
        negocios.flush();
        entityManager.clear();
    }

    private void whenSeDefineLaConfiguracionComercialDelGratuito() {
        PlanEntity plan = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow();
        plan.setConfigurado(true);
        plan.setMoneda("ARS");
        plan.setLimiteProyectos(3);
        plan.setLimiteRegistros(100L);
        plan.setLimitePaginasIaMensuales(10);
        plan.setFunciones(List.of("FUNCION_DE_PRUEBA"));
        planes.saveAndFlush(plan);
        entityManager.clear();
        planLeido = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow();
    }

    private void whenSeActualizaElGratuitoCon(String asignacion) {
        UUID planId = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();
        error = catchThrowable(() -> jdbcTemplate.update("UPDATE plan SET " + asignacion + " WHERE id = ?", planId));
    }

    private void whenSeGuardaUnaSuscripcionQueTerminaAntesDeEmpezar() {
        UUID planId = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();
        error = catchThrowable(() -> suscripciones.saveAndFlush(SuscripcionEntity.builder()
                .negocioId(negocioA)
                .planId(planId)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA)
                .finEn(AHORA.minusSeconds(1))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()));
    }

    private void whenSeGuardaUnaSuscripcionParaUnNegocioInexistente() {
        UUID planId = planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();
        error = catchThrowable(() -> suscripciones.saveAndFlush(SuscripcionEntity.builder()
                .negocioId(NEGOCIO_INEXISTENTE)
                .planId(planId)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()));
    }

    // --- assert ---
    private void thenElGratuitoTieneSusCuotasAprobadas() {
        assertThat(gratuito.getNombre()).isEqualTo("Gratuito");
        assertThat(gratuito.getPrecioMensual()).isEqualByComparingTo(BigDecimal.ZERO);
        thenCommercialValuesPending(gratuito);
        assertThat(gratuito.getLimitesConfigurados()).isTrue();
        assertThat(gratuito.getLimiteProyectos()).isEqualTo(1);
        assertThat(gratuito.getLimiteRegistros()).isEqualTo(1000L);
        assertThat(gratuito.getLimitePaginasIaMensuales()).isEqualTo(10);
        assertThat(gratuito.getLimiteObjetivosIaMensuales()).isEqualTo(5);
        assertThat(gratuito.getFunciones()).isEmpty();
    }

    private void thenElPremiumTieneSuPrecioYSusCuotasAprobadas() {
        assertThat(premium.getNombre()).isEqualTo("Premium");
        assertThat(premium.getPrecioMensual()).isEqualByComparingTo("14900.00");
        assertThat(premium.getMoneda()).isEqualTo("ARS");
        assertThat(premium.getConfigurado()).isTrue();
        assertThat(premium.getLimitesConfigurados()).isTrue();
        assertThat(premium.getLimiteProyectos()).isNull();
        assertThat(premium.getLimiteRegistros()).isNull();
        assertThat(premium.getLimitePaginasIaMensuales()).isEqualTo(100);
        assertThat(premium.getLimiteObjetivosIaMensuales()).isEqualTo(20);
        assertThat(premium.getFunciones()).containsExactly(
                "RECETAS_COSTOS", "REPORTES_FILTRADOS", "EXPORTAR_EXCEL", "AVISOS_COMPLETOS", "ZONA_COMPLETA");
    }

    private void thenSeLeeLaSuscripcionGratuitaConSusFechas() {
        assertThat(suscripcionLeida.getId()).isNotNull();
        assertThat(suscripcionLeida.getNegocioId()).isEqualTo(NegocioId.de(negocioA));
        assertThat(suscripcionLeida.getPlanId())
                .isEqualTo(PlanId.de(planes.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId()));
        assertThat(suscripcionLeida.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(suscripcionLeida.getInicioEn()).isNotNull();
        assertThat(suscripcionLeida.getFinEn()).isNull();
        assertThat(suscripcionLeida.getCreadoEn()).isEqualTo(suscripcionLeida.getInicioEn());
        assertThat(suscripcionLeida.getActualizadoEn()).isEqualTo(suscripcionLeida.getInicioEn());
    }

    private void thenNoSeEncuentraLaDeB() {
        assertThat(suscripcionBuscada).isEmpty();
    }

    private void thenSigueLaDeA() {
        assertThat(suscripciones.findByNegocioId(negocioA)).isPresent();
    }

    private void thenLaBaseLaRechaza() {
        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void thenSeBorraLaSuscripcionPeroNoElCatalogo() {
        assertThat(suscripciones.findById(suscripcionId)).isEmpty();
        assertThat(planes.findByCodigo(CodigoPlan.GRATUITO)).isPresent();
    }

    private void thenSeRelenLimitesMonedaFuncionesYPrecio() {
        assertThat(planLeido.getConfigurado()).isTrue();
        assertThat(planLeido.getMoneda()).isEqualTo("ARS");
        assertThat(planLeido.getLimiteProyectos()).isEqualTo(3);
        assertThat(planLeido.getLimiteRegistros()).isEqualTo(100L);
        assertThat(planLeido.getLimitePaginasIaMensuales()).isEqualTo(10);
        assertThat(planLeido.getFunciones()).containsExactly("FUNCION_DE_PRUEBA");
        assertThat(planLeido.getPrecioMensual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void thenCommercialValuesPending(PlanEntity plan) {
        assertThat(plan.getConfigurado()).isFalse();
        assertThat(plan.getMoneda()).isNull();
    }
}
