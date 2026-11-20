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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** El acceso restringido a la IA que controla {@link ControlPlanService#exigirIa}. */
class ControlPlanAccesoIaTest {

    private static final NegocioId NEGOCIO = NegocioId.nuevo();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T03:05:00Z");
    private static final String DUENIO_HABILITADO = "dueño@example.com";
    private static final String DUENIO_AJENO = "otro@example.com";

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final SuscripcionRepository suscripciones = mock(SuscripcionRepository.class);

    private ControlPlanService servicio;
    private Throwable rechazo;

    @Test
    @DisplayName("Sin restringir, la IA no consulta al dueño")
    void sinRestringirLaIaNoConsultaAlDuenio() {
        dadoUnServicioConAcceso(AccesoIa.LIBRE);

        cuandoSeExigeLaIa();

        entoncesSeDejaPasarSinConsultarAlDuenio();
    }

    @Test
    @DisplayName("Restringido, deja pasar al dueño con email habilitado")
    void restringidoDejaPasarAlDuenioConEmailHabilitado() {
        dadoUnServicioRestringidoAl(DUENIO_HABILITADO);
        dadoQueElDuenioDelNegocioEs(DUENIO_HABILITADO);

        cuandoSeExigeLaIa();

        entoncesSeDejaPasar();
    }

    @Test
    @DisplayName("Restringido, rechaza al dueño con un email que no está en la lista")
    void restringidoRechazaAlDuenioConUnEmailQueNoEstaEnLaLista() {
        dadoUnServicioRestringidoAl(DUENIO_HABILITADO);
        dadoQueElDuenioDelNegocioEs(DUENIO_AJENO);

        cuandoSeExigeLaIa();

        entoncesLaIaNoEstaHabilitada();
    }

    @Test
    @DisplayName("Restringido, rechaza un negocio sin dueño conocido")
    void restringidoRechazaUnNegocioSinDuenioConocido() {
        dadoUnServicioRestringidoAl(DUENIO_HABILITADO);
        dadoUnNegocioSinDuenioConocido();

        cuandoSeExigeLaIa();

        entoncesLaIaNoEstaHabilitada();
    }

    @Test
    @DisplayName("Restringido, validar las páginas de IA rechaza antes de mirar el plan")
    void restringidoValidarLasPaginasDeIaRechazaAntesDeMirarElPlan() {
        dadoUnServicioConAcceso(new AccesoIa(true, Set.of()));
        dadoQueElDuenioDelNegocioEs(DUENIO_AJENO);

        cuandoSeValidanLasPaginasDeIa();

        entoncesSeRechazaSinMirarLaSuscripcion();
    }

    // --- dado ---

    private void dadoUnServicioConAcceso(AccesoIa acceso) {
        servicio = new ControlPlanService(mock(PlanRepository.class), suscripciones, mock(UsoPlanRepository.class),
                mock(AccesoContenidoRepository.class), mock(PruebaPremiumRepository.class),
                new TransaccionDirecta(), negocios, acceso);
    }

    private void dadoUnServicioRestringidoAl(String duenioHabilitado) {
        dadoUnServicioConAcceso(new AccesoIa(true, Set.of(duenioHabilitado)));
    }

    private void dadoQueElDuenioDelNegocioEs(String email) {
        when(negocios.findEmailDuenioById(NEGOCIO)).thenReturn(Optional.of(email));
    }

    private void dadoUnNegocioSinDuenioConocido() {
        when(negocios.findEmailDuenioById(NEGOCIO)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeExigeLaIa() {
        rechazo = catchThrowable(() -> servicio.exigirIa(NEGOCIO));
    }

    private void cuandoSeValidanLasPaginasDeIa() {
        rechazo = catchThrowable(() -> servicio.validarPaginasIa(NEGOCIO, 1, AHORA));
    }

    // --- entonces ---

    private void entoncesSeDejaPasar() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeDejaPasarSinConsultarAlDuenio() {
        entoncesSeDejaPasar();
        verifyNoInteractions(negocios);
    }

    private void entoncesLaIaNoEstaHabilitada() {
        assertThat(rechazo).isInstanceOfSatisfying(RestriccionPlanException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo("IA_NO_HABILITADA");
            assertThat(e.getMessage()).isEqualTo(AccesoIa.MENSAJE);
        });
    }

    private void entoncesSeRechazaSinMirarLaSuscripcion() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class);
        verifyNoInteractions(suscripciones);
    }
}
