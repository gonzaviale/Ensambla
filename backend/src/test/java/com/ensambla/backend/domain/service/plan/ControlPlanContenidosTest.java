package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.support.PlanTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** El acceso a los contenidos con cupo del plan gratuito (avisos, zona) que controla {@link ControlPlanService}. */
class ControlPlanContenidosTest {

    private static final NegocioId NEGOCIO = NegocioId.nuevo();
    private static final UsuarioId USUARIO = UsuarioId.nuevo();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T03:05:00Z");
    private static final LocalDate PERIODO = LocalDate.of(2026, 11, 1);

    private final PlanRepository planes = mock(PlanRepository.class);
    private final SuscripcionRepository suscripciones = mock(SuscripcionRepository.class);
    private final UsoPlanRepository uso = mock(UsoPlanRepository.class);
    private final AccesoContenidoRepository acceso = mock(AccesoContenidoRepository.class);
    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final ControlPlanService servicio = new ControlPlanService(planes, suscripciones, uso, acceso,
            mock(PruebaPremiumRepository.class), new TransaccionDirecta(), negocios, AccesoIa.LIBRE);

    @Test
    @DisplayName("Gratuito admite seis claves distintas y conserva el orden sin cobrar duplicados")
    void seleccionarShouldLimitUniqueNewContent() {
        givenPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO)).thenReturn(new HashSet<>());

        AccesoContenidoItem resultado = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.AVISOS,
                List.of("a", "b", "a", "c", "d", "e", "f", "g"), AHORA);

        assertThat(resultado.autorizadas()).containsExactly("a", "b", "c", "d", "e", "f");
        assertThat(resultado.usadas()).isEqualTo(6);
        assertThat(resultado.limite()).isEqualTo(6);
        assertThat(resultado.total()).isEqualTo(7);
        assertThat(resultado.bloqueadas()).isEqualTo(1);
        assertThat(resultado.finPeriodo()).isEqualTo(OffsetDateTime.parse("2026-12-01T03:00:00Z"));
        verify(uso).bloquearNegocio(NEGOCIO);
        verify(acceso).registrar(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO, "a", AHORA);
    }

    @Test
    @DisplayName("Reconsultar lo desbloqueado no libera cupo para el séptimo contenido")
    void seleccionarShouldKeepExistingKeysWithoutRotatingQuota() {
        givenPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.ZONA, PERIODO))
                .thenReturn(new HashSet<>(Set.of("a", "b", "c", "d", "e", "f")));

        AccesoContenidoItem resultado = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.ZONA,
                List.of("a", "g"), AHORA.withOffsetSameInstant(ZoneOffset.ofHours(-3)));

        assertThat(resultado.autorizadas()).containsExactly("a");
        assertThat(resultado.bloqueadas()).isEqualTo(1);
        assertThat(resultado.usadas()).isEqualTo(6);
    }

    @Test
    @DisplayName("Premium entrega todo y no gasta los desbloqueos gratuitos")
    void seleccionarShouldReturnAllForPremium() {
        givenPlan(CodigoPlan.PREMIUM);

        AccesoContenidoItem resultado = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.AVISOS,
                List.of("a", "b", "c", "d"), AHORA);

        assertThat(resultado.autorizadas()).hasSize(4);
        assertThat(resultado.limite()).isNull();
        assertThat(resultado.bloqueadas()).isZero();
        verifyNoInteractions(acceso);
    }

    @Test
    @DisplayName("Una acción directa sobre el séptimo aviso devuelve una restricción comercial")
    void exigirShouldRejectSeventhContent() {
        givenPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO))
                .thenReturn(new HashSet<>(Set.of("a", "b", "c", "d", "e", "f")));

        assertThatThrownBy(() -> servicio.exigir(NEGOCIO, TipoContenidoPlan.AVISOS, "g", AHORA))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("6 contenidos")
                .extracting("codigo").isEqualTo("LIMITE_CONTENIDO_AVISOS");
    }

    @Test
    @DisplayName("Una acción sobre un aviso ya desbloqueado sigue disponible")
    void exigirShouldAllowPreviouslyUnlockedContent() {
        givenPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO))
                .thenReturn(new HashSet<>(Set.of("a", "b", "c", "d", "e", "f")));

        assertThatCode(() -> servicio.exigir(NEGOCIO, TipoContenidoPlan.AVISOS, "a", AHORA))
                .doesNotThrowAnyException();
    }

    // --- arrange ---
    /** La cuenta dueña del negocio tiene una suscripción vigente al plan con ese código. */
    private void givenPlan(CodigoPlan codigo) {
        Plan plan = PlanTestData.plan();
        plan.setCodigo(codigo);
        Suscripcion suscripcion = PlanTestData.suscripcion();
        suscripcion.setInicioEn(AHORA.minusDays(1));
        suscripcion.setFinEn(AHORA.plusMonths(1));
        when(negocios.obtenerDuenioId(NEGOCIO)).thenReturn(USUARIO);
        when(suscripciones.findByUsuarioId(USUARIO)).thenReturn(Optional.of(suscripcion));
        when(planes.findById(suscripcion.getPlanId())).thenReturn(Optional.of(plan));
    }
}
