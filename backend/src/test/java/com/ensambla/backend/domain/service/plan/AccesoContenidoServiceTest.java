package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccesoContenidoServiceTest {

    private static final UUID NEGOCIO = UUID.randomUUID();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T00:05:00Z");
    private static final LocalDate PERIODO = LocalDate.of(2026, 11, 1);

    private final ControlPlanService control = mock(ControlPlanService.class);
    private final UsoPlanRepository uso = mock(UsoPlanRepository.class);
    private final AccesoContenidoRepository acceso = mock(AccesoContenidoRepository.class);
    private final AccesoContenidoService servicio = new AccesoContenidoService(control, uso, acceso, new TransaccionDirecta());

    @Test
    @DisplayName("Gratuito admite tres claves distintas y conserva el orden sin cobrar duplicados")
    void seleccionarShouldLimitUniqueNewContent() {
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO)).thenReturn(new HashSet<>());

        AccesoContenidoItem resultado = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.AVISOS,
                List.of("a", "b", "a", "c", "d"), AHORA);

        assertThat(resultado.autorizadas()).containsExactly("a", "b", "c");
        assertThat(resultado.usadas()).isEqualTo(3);
        assertThat(resultado.limite()).isEqualTo(3);
        assertThat(resultado.total()).isEqualTo(4);
        assertThat(resultado.bloqueadas()).isEqualTo(1);
        assertThat(resultado.finPeriodo()).isEqualTo(OffsetDateTime.parse("2026-12-01T00:00:00Z"));
        verify(uso).bloquearNegocio(NEGOCIO);
        verify(acceso).registrar(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO, "a", AHORA);
    }

    @Test
    @DisplayName("Reconsultar lo desbloqueado no libera cupo para el cuarto contenido")
    void seleccionarShouldKeepExistingKeysWithoutRotatingQuota() {
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.ZONA, PERIODO))
                .thenReturn(new HashSet<>(Set.of("a", "b", "c")));

        AccesoContenidoItem resultado = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.ZONA,
                List.of("a", "d"), AHORA.withOffsetSameInstant(java.time.ZoneOffset.ofHours(-3)));

        assertThat(resultado.autorizadas()).containsExactly("a");
        assertThat(resultado.bloqueadas()).isEqualTo(1);
        assertThat(resultado.usadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Premium entrega todo y no gasta los desbloqueos gratuitos")
    void seleccionarShouldReturnAllForPremium() {
        when(control.esPremium(NEGOCIO, AHORA)).thenReturn(true);

        AccesoContenidoItem resultado = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.AVISOS,
                List.of("a", "b", "c", "d"), AHORA);

        assertThat(resultado.autorizadas()).hasSize(4);
        assertThat(resultado.limite()).isNull();
        assertThat(resultado.bloqueadas()).isZero();
        verifyNoInteractions(acceso);
    }

    @Test
    @DisplayName("Una acción directa sobre el cuarto aviso devuelve una restricción comercial")
    void exigirShouldRejectFourthContent() {
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO))
                .thenReturn(new HashSet<>(Set.of("a", "b", "c")));

        assertThatThrownBy(() -> servicio.exigir(NEGOCIO, TipoContenidoPlan.AVISOS, "d", AHORA))
                .isInstanceOf(RestriccionPlanException.class)
                .extracting("codigo").isEqualTo("LIMITE_CONTENIDO_AVISOS");
    }

    @Test
    @DisplayName("Una acción sobre un aviso ya desbloqueado sigue disponible")
    void exigirShouldAllowPreviouslyUnlockedContent() {
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO))
                .thenReturn(new HashSet<>(Set.of("a", "b", "c")));

        assertThatCode(() -> servicio.exigir(NEGOCIO, TipoContenidoPlan.AVISOS, "a", AHORA))
                .doesNotThrowAnyException();
    }
}
