package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** El acceso restringido a la IA que controla {@link ControlPlanService#exigirIa}. */
class ControlPlanAccesoIaTest {

    private static final NegocioId NEGOCIO = NegocioId.nuevo();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T03:05:00Z");

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final SuscripcionRepository suscripciones = mock(SuscripcionRepository.class);

    @Test
    @DisplayName("Sin restringir, la IA no consulta al dueño")
    void sinRestringirLaIANoConsultaAlDueno() {
        ControlPlanService servicio = dadoServicio(AccesoIa.LIBRE);

        assertThatCode(() -> servicio.exigirIa(NEGOCIO)).doesNotThrowAnyException();
        verifyNoInteractions(negocios);
    }

    @Test
    @DisplayName("Restringido, deja pasar al dueño con email habilitado")
    void restringidoDejaPasarAlDuenoConEmailHabilitado() {
        ControlPlanService servicio = dadoServicio(new AccesoIa(true, Set.of("dueño@example.com")));
        when(negocios.findEmailDuenioById(NEGOCIO)).thenReturn(Optional.of("dueño@example.com"));

        assertThatCode(() -> servicio.exigirIa(NEGOCIO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Restringido, rechaza al dueño con un email que no está en la lista")
    void restringidoRechazaAlDuenoConUnEmailQueNoEstaEnLaLista() {
        ControlPlanService servicio = dadoServicio(new AccesoIa(true, Set.of("dueño@example.com")));
        when(negocios.findEmailDuenioById(NEGOCIO)).thenReturn(Optional.of("otro@example.com"));

        entoncesIaNoHabilitada(servicio);
    }

    @Test
    @DisplayName("Restringido, rechaza un negocio sin dueño conocido")
    void restringidoRechazaUnNegocioSinDuenoConocido() {
        ControlPlanService servicio = dadoServicio(new AccesoIa(true, Set.of("dueño@example.com")));
        when(negocios.findEmailDuenioById(NEGOCIO)).thenReturn(Optional.empty());

        entoncesIaNoHabilitada(servicio);
    }

    @Test
    @DisplayName("Restringido, validar las páginas de IA rechaza antes de mirar el plan")
    void restringidoValidarLasPaginasDeIARechazaAntesDeMirarElPlan() {
        ControlPlanService servicio = dadoServicio(new AccesoIa(true, Set.of()));
        when(negocios.findEmailDuenioById(NEGOCIO)).thenReturn(Optional.of("otro@example.com"));

        assertThatThrownBy(() -> servicio.validarPaginasIa(NEGOCIO, 1, AHORA))
                .isInstanceOf(RestriccionPlanException.class);
        verifyNoInteractions(suscripciones);
    }

    // --- arrange / assert ---
    private ControlPlanService dadoServicio(AccesoIa acceso) {
        return new ControlPlanService(mock(PlanRepository.class), suscripciones, mock(UsoPlanRepository.class),
                mock(AccesoContenidoRepository.class), mock(PruebaPremiumRepository.class),
                new TransaccionDirecta(), negocios, acceso);
    }

    private void entoncesIaNoHabilitada(ControlPlanService servicio) {
        assertThatThrownBy(() -> servicio.exigirIa(NEGOCIO))
                .isInstanceOfSatisfying(RestriccionPlanException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("IA_NO_HABILITADA");
                    assertThat(e.getMessage()).isEqualTo(AccesoIa.MENSAJE);
                });
    }
}
