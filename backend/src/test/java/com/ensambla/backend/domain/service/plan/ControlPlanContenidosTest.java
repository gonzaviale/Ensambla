package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** El acceso a los contenidos con cupo del plan gratuito (avisos, zona) que controla {@link ControlPlanService}. */
class ControlPlanContenidosTest {

    private static final NegocioId NEGOCIO = NegocioId.nuevo();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T03:05:00Z");
    private static final List<String> AVISOS_DESBLOQUEADOS = List.of(
            "e0000000-0000-4000-8000-000000000001", "e0000000-0000-4000-8000-000000000002",
            "e0000000-0000-4000-8000-000000000003", "e0000000-0000-4000-8000-000000000004",
            "e0000000-0000-4000-8000-000000000005", "e0000000-0000-4000-8000-000000000006");
    private static final Set<String> SEIS_CLAVES = Set.of("a", "b", "c", "d", "e", "f");
    private static final LocalDate PERIODO = LocalDate.of(2026, 11, 1);

    private final PlanRepository planes = mock(PlanRepository.class);
    private final SuscripcionRepository suscripciones = mock(SuscripcionRepository.class);
    private final UsoPlanRepository uso = mock(UsoPlanRepository.class);
    private final AccesoContenidoRepository acceso = mock(AccesoContenidoRepository.class);
    private final ControlPlanService servicio = new ControlPlanService(planes, suscripciones, uso, acceso,
            mock(PruebaPremiumRepository.class), new TransaccionDirecta(), mock(NegocioRepository.class),
            AccesoIa.LIBRE);

    private AccesoContenidoItem seleccion;
    private List<AvisoId> avisosAutorizados;
    private Throwable rechazo;
    private AvisoId avisoYaDesbloqueado;
    private AvisoId avisoNuevo;

    @Test
    @DisplayName("El plan gratuito admite seis claves distintas en orden sin cobrar duplicados")
    void elPlanGratuitoAdmiteSeisClavesDistintasEnOrdenSinCobrarDuplicados() {
        dadoUnNegocioGratuitoSinContenidosDesbloqueados();

        cuandoSeSeleccionanSieteAvisosConUnoRepetido();

        entoncesSeAutorizanSeisAvisosEnOrdenYSeRegistraSuUso();
    }

    @Test
    @DisplayName("Reconsultar lo desbloqueado no libera cupo para un séptimo contenido")
    void reconsultarLoDesbloqueadoNoLiberaCupoParaUnSeptimoContenido() {
        dadoUnNegocioGratuitoConSeisZonasDesbloqueadas();

        cuandoSeReconsultaUnaZonaDesbloqueadaYSePideUnaNueva();

        entoncesSoloQuedaAutorizadaLaZonaYaDesbloqueada();
    }

    @Test
    @DisplayName("El plan premium entrega todo sin gastar los desbloqueos gratuitos")
    void elPlanPremiumEntregaTodoSinGastarLosDesbloqueosGratuitos() {
        dadoUnNegocioPremium();

        cuandoSeSeleccionanCuatroAvisos();

        entoncesSeAutorizanLosCuatroSinLimiteNiConsumirCupo();
    }

    @Test
    @DisplayName("Una acción directa sobre el séptimo aviso se rechaza por restricción comercial")
    void unaAccionDirectaSobreElSeptimoAvisoSeRechazaPorRestriccionComercial() {
        dadoUnNegocioGratuitoConSeisClavesDeAvisosDesbloqueadas();

        cuandoSeExigeAccesoAUnSeptimoAvisoPorClave();

        entoncesSeRechazaPorElLimiteDeContenidosDeAvisos();
    }

    @Test
    @DisplayName("Una acción sobre un aviso ya desbloqueado por clave sigue disponible")
    void unaAccionSobreUnAvisoYaDesbloqueadoPorClaveSigueDisponible() {
        dadoUnNegocioGratuitoConSeisClavesDeAvisosDesbloqueadas();

        cuandoSeExigeAccesoAUnAvisoDesbloqueadoPorClave();

        entoncesLaAccionSigueDisponible();
    }

    @Test
    @DisplayName("Los avisos desbloqueados quedan autorizados y el resto no, en el orden pedido")
    void losAvisosDesbloqueadosQuedanAutorizadosYElRestoNoEnElOrdenPedido() {
        dadoUnNegocioGratuitoConSeisAvisosDesbloqueados();

        cuandoSeConsultanUnAvisoNuevoYUnoYaDesbloqueado();

        entoncesSoloQuedaAutorizadoElAvisoYaDesbloqueado();
    }

    @Test
    @DisplayName("Una acción sobre un aviso ya desbloqueado sigue disponible sin la clave guardada")
    void unaAccionSobreUnAvisoYaDesbloqueadoSigueDisponibleSinConocerLaClaveGuardada() {
        dadoUnNegocioGratuitoConSeisAvisosDesbloqueados();

        cuandoSeExigeAccesoAUnAvisoYaDesbloqueado();

        entoncesLaAccionSigueDisponible();
    }

    @Test
    @DisplayName("Una acción sobre un séptimo aviso se rechaza")
    void unaAccionSobreUnSeptimoAvisoSeRechaza() {
        dadoUnNegocioGratuitoConSeisAvisosDesbloqueados();

        cuandoSeExigeAccesoAUnSeptimoAviso();

        entoncesSeRechazaPorRestriccionDelPlan();
    }

    private void dadoUnNegocioGratuitoSinContenidosDesbloqueados() {
        suscripcionVigenteAlPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO)).thenReturn(new HashSet<>());
    }

    private void dadoUnNegocioGratuitoConSeisZonasDesbloqueadas() {
        suscripcionVigenteAlPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.ZONA, PERIODO)).thenReturn(new HashSet<>(SEIS_CLAVES));
    }

    private void dadoUnNegocioPremium() {
        suscripcionVigenteAlPlan(CodigoPlan.PREMIUM);
    }

    private void dadoUnNegocioGratuitoConSeisClavesDeAvisosDesbloqueadas() {
        suscripcionVigenteAlPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO)).thenReturn(new HashSet<>(SEIS_CLAVES));
    }

    private void dadoUnNegocioGratuitoConSeisAvisosDesbloqueados() {
        suscripcionVigenteAlPlan(CodigoPlan.GRATUITO);
        when(acceso.autorizadas(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO))
                .thenReturn(new HashSet<>(AVISOS_DESBLOQUEADOS));
        avisoYaDesbloqueado = AvisoId.de(AVISOS_DESBLOQUEADOS.getFirst());
        avisoNuevo = AvisoId.nuevo();
    }

    private void cuandoSeSeleccionanSieteAvisosConUnoRepetido() {
        seleccion = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.AVISOS,
                List.of("a", "b", "a", "c", "d", "e", "f", "g"), AHORA);
    }

    private void cuandoSeReconsultaUnaZonaDesbloqueadaYSePideUnaNueva() {
        seleccion = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.ZONA, List.of("a", "g"),
                AHORA.withOffsetSameInstant(ZoneOffset.ofHours(-3)));
    }

    private void cuandoSeSeleccionanCuatroAvisos() {
        seleccion = servicio.seleccionar(NEGOCIO, TipoContenidoPlan.AVISOS, List.of("a", "b", "c", "d"), AHORA);
    }

    private void cuandoSeExigeAccesoAUnSeptimoAvisoPorClave() {
        rechazo = catchThrowable(() -> servicio.exigir(NEGOCIO, TipoContenidoPlan.AVISOS, "g", AHORA));
    }

    private void cuandoSeExigeAccesoAUnAvisoDesbloqueadoPorClave() {
        rechazo = catchThrowable(() -> servicio.exigir(NEGOCIO, TipoContenidoPlan.AVISOS, "a", AHORA));
    }

    private void cuandoSeConsultanUnAvisoNuevoYUnoYaDesbloqueado() {
        avisosAutorizados = servicio.avisosAutorizados(NEGOCIO, List.of(avisoNuevo, avisoYaDesbloqueado), AHORA);
    }

    private void cuandoSeExigeAccesoAUnAvisoYaDesbloqueado() {
        rechazo = catchThrowable(() -> servicio.exigirAccesoAlAviso(NEGOCIO, avisoYaDesbloqueado, AHORA));
    }

    private void cuandoSeExigeAccesoAUnSeptimoAviso() {
        rechazo = catchThrowable(() -> servicio.exigirAccesoAlAviso(NEGOCIO, AvisoId.nuevo(), AHORA));
    }

    private void entoncesSeAutorizanSeisAvisosEnOrdenYSeRegistraSuUso() {
        entoncesElCupoGratuitoQuedaAgotadoConSeisAvisosEnOrden();
        verify(uso).bloquearNegocio(NEGOCIO);
        verify(acceso).registrar(NEGOCIO, TipoContenidoPlan.AVISOS, PERIODO, "a", AHORA);
    }

    private void entoncesElCupoGratuitoQuedaAgotadoConSeisAvisosEnOrden() {
        assertThat(seleccion.autorizadas()).containsExactly("a", "b", "c", "d", "e", "f");
        assertThat(seleccion.usadas()).isEqualTo(6);
        assertThat(seleccion.limite()).isEqualTo(6);
        assertThat(seleccion.total()).isEqualTo(7);
        assertThat(seleccion.bloqueadas()).isEqualTo(1);
        assertThat(seleccion.finPeriodo()).isEqualTo(OffsetDateTime.parse("2026-12-01T03:00:00Z"));
    }

    private void entoncesSoloQuedaAutorizadaLaZonaYaDesbloqueada() {
        assertThat(seleccion.autorizadas()).containsExactly("a");
        assertThat(seleccion.bloqueadas()).isEqualTo(1);
        assertThat(seleccion.usadas()).isEqualTo(6);
    }

    private void entoncesSeAutorizanLosCuatroSinLimiteNiConsumirCupo() {
        assertThat(seleccion.autorizadas()).hasSize(4);
        assertThat(seleccion.limite()).isNull();
        assertThat(seleccion.bloqueadas()).isZero();
        verifyNoInteractions(acceso);
    }

    private void entoncesSeRechazaPorElLimiteDeContenidosDeAvisos() {
        assertThat(rechazo)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("6 contenidos")
                .extracting("codigo").isEqualTo("LIMITE_CONTENIDO_AVISOS");
    }

    private void entoncesLaAccionSigueDisponible() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSoloQuedaAutorizadoElAvisoYaDesbloqueado() {
        assertThat(avisosAutorizados).containsExactly(avisoYaDesbloqueado);
    }

    private void entoncesSeRechazaPorRestriccionDelPlan() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class);
    }

    /** El negocio tiene una suscripción vigente al plan con ese código. */
    private void suscripcionVigenteAlPlan(CodigoPlan codigo) {
        Plan plan = PlanTestData.plan();
        plan.setCodigo(codigo);
        Suscripcion suscripcion = PlanTestData.suscripcion();
        suscripcion.setInicioEn(AHORA.minusDays(1));
        suscripcion.setFinEn(AHORA.plusMonths(1));
        when(suscripciones.findByNegocioId(NEGOCIO)).thenReturn(Optional.of(suscripcion));
        when(planes.findById(suscripcion.getPlanId())).thenReturn(Optional.of(plan));
    }
}
