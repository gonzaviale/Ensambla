package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.CuotaObjetivosIa;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.PLAN_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlPlanServiceTest {

    private static final OffsetDateTime MADRUGADA_UTC_DEL_PRIMERO_DE_OCTUBRE =
            OffsetDateTime.parse("2026-10-01T02:30:00Z");
    private static final OffsetDateTime MEDIADOS_DE_OCTUBRE = OffsetDateTime.parse("2026-10-20T12:00:00Z");
    private static final int LIMITE_DIARIO_DE_PAGINAS = 100;
    private static final long PAGINAS_YA_USADAS_DEL_MES = 10L;
    private static final long REGISTROS_CASI_AL_TOPE = 99L;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @Mock
    private PruebaPremiumRepository pruebaPremiumRepository;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @Mock
    private NegocioRepository negocioRepository;

    @Spy
    private AccesoIa accesoIa = AccesoIa.LIBRE;

    @InjectMocks
    private ControlPlanService controlPlanService;

    private Throwable error;
    private boolean reservaConcedida;
    private BooleanSupplier reservaDiaria;
    private PruebaPremium pruebaVigente;
    private CuotaObjetivosIa cuota;

    @Test
    @DisplayName("Bloquea el negocio antes de contar y autorizar un sistema")
    void validarAltaProyectoShouldLockBeforeCounting() {
        givenActivePlan();

        whenSeValidaElAltaDeUnProyecto();

        thenSeBloqueaElNegocioAntesDeContar();
    }

    @Test
    @DisplayName("Rechaza todas las filas del lote cuando no entran en la cuota")
    void validarAltaRegistrosShouldRejectWholeBatch() {
        givenActivePlan();
        givenRegistrosCasiAlTope();

        whenSeValidaElAltaDeDosRegistros();

        thenSeRechazaPorElPlan();
    }

    @Test
    @DisplayName("Calcula el mes por el día argentino aunque en UTC ya sea el mes siguiente")
    void validarPaginasIaShouldUseArgentinaMonth() {
        givenUnaSuscripcionIniciadaElMesPasado();

        whenSeValidaUnaPaginaDeIaEn(MADRUGADA_UTC_DEL_PRIMERO_DE_OCTUBRE);

        thenSeCuentanLasPaginasDeSeptiembreConElNegocioBloqueado();
    }

    @Test
    @DisplayName("Una función incluida se permite sin bloquear ni contar las altas")
    void validarFuncionShouldAllowEnabledFeature() {
        givenUnPlanConRecetasIncluidas();

        whenSeValidaLaFuncionRecetas();

        thenSePermiteSinBloquearNiContar();
    }

    @Test
    @DisplayName("La suscripción vencida usa el gratuito aunque todavía figure activa en persistencia")
    void validarFuncionShouldFallBackAtExactExpiry() {
        givenUnaSuscripcionQueVenceJusto();

        whenSeValidaLaFuncionRecetas();

        thenSeRechazaPorFaltaDePremiumActivo();
    }

    @Test
    @DisplayName("Una suscripción cancelada conserva los límites de alta del gratuito")
    void validarAltaProyectoShouldUseFreePlanWhenCancelled() {
        givenUnaSuscripcionCancelada();

        whenSeValidaElAltaDeUnProyecto();

        thenSeUsaElPlanGratuito();
    }

    @Test
    @DisplayName("Un negocio sin suscripción no recibe capacidades por defecto")
    void validarFuncionShouldRejectMissingSubscription() {
        givenUnNegocioSinSuscripcion();

        whenSeValidaLaFuncionRecetas();

        thenSeRechazaPorSuscripcionInexistenteSinConsultarPlanes();
    }

    @Test
    @DisplayName("No habilita un plan referido que ya no existe")
    void validarFuncionShouldRejectMissingAssignedPlan() {
        givenUnPlanAsignadoQueYaNoExiste();

        whenSeValidaLaFuncionRecetas();

        thenSeRechazaPorPlanInexistente("Plan no encontrado");
    }

    @Test
    @DisplayName("Una suscripción vencida no habilita funciones si falta el catálogo gratuito")
    void validarFuncionShouldRejectMissingFallbackPlan() {
        givenUnaSuscripcionVencidaSinPlanGratuito();

        whenSeValidaLaFuncionRecetas();

        thenSeRechazaPorPlanInexistente("Plan gratuito no encontrado");
    }

    @Test
    @DisplayName("Una prueba sin historial persistido falla sin autorizar páginas")
    void validarPaginasIaShouldRejectTrialWithoutHistory() {
        givenUnaPruebaSinHistorial();

        whenSeValidaUnaPaginaDeIaEn(INSTANTE);

        thenSeRechazaPorPlanInexistente("Prueba no encontrada");
    }

    @Test
    @DisplayName("Una foto inmediata valida el mes, reserva en el día argentino con el límite diario y no lanza si entra")
    void reservarPaginaIaInmediataShouldReserveInTheArgentinaDay() {
        givenActivePlan();
        givenQueLaReservaInmediataEntra();

        whenSeReservaUnaPaginaInmediataEnArgentina();

        thenSeReservaLaPaginaEnElDiaArgentino();
    }

    @Test
    @DisplayName("Una foto inmediata con el límite diario agotado responde la restricción del día")
    void reservarPaginaIaInmediataShouldRejectWhenDailyLimitIsReached() {
        givenActivePlan();
        givenQueLaReservaInmediataNoEntra();

        whenSeReservaUnaPaginaInmediata();

        thenSeRechazaPorElLimiteDiario();
    }

    @Test
    @DisplayName("La reserva de páginas tampoco admite un negocio sin suscripción")
    void reservarPaginasIaShouldRejectMissingSubscription() {
        givenUnaReservaDiariaQueEntra();

        whenSeReservanPaginas(1);

        thenSeRechazaPorPlanInexistente("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Una prueba vencida usa la cuota mensual gratuita")
    void reservarPaginasIaShouldUseTheFreeMonthlyQuotaWhenTheTrialExpired() {
        givenUnaPruebaVencida();
        givenUnaReservaDiariaQueEntra();

        whenSeReservanPaginas(1);

        thenSeConcedeLaReserva();
    }

    @Test
    @DisplayName("Una prueba vencida no incrementa el historial de prueba")
    void reservarPaginasIaShouldNotTouchTheTrialHistoryWhenTheTrialExpired() {
        givenUnaPruebaVencida();
        givenUnaReservaDiariaQueEntra();

        whenSeReservanPaginas(1);

        thenNoSeTocaElHistorialDePrueba();
    }

    @Test
    @DisplayName("Valida la cuota de la prueba antes de la reserva diaria y, si entra, la descuenta de la prueba")
    void reservarPaginasIaShouldValidateReserveAndChargeTrialInOrder() {
        givenUnaPruebaVigente();
        givenUnaReservaDiariaEspiadaQueEntra();

        whenSeReservanPaginas(2);

        thenSeValidaLaReservaYSeDescuentaDeLaPruebaEnOrden();
    }

    @Test
    @DisplayName("Una reserva diaria que no entra no descuenta páginas de la prueba")
    void reservarPaginasIaShouldNotChargeTrialWhenReservationFails() {
        givenUnaPruebaVigente();
        givenUnaReservaDiariaQueNoEntra();

        whenSeReservanPaginas(2);

        thenNoSeConcedeLaReservaNiSeDescuentaDeLaPrueba();
    }

    @Test
    @DisplayName("Un rechazo de la cuota mensual no llega a hacer la reserva diaria")
    void reservarPaginasIaShouldNotReserveWhenMonthlyQuotaIsFull() {
        givenActivePlan();
        givenLaCuotaMensualDePaginasLlena();
        givenUnaReservaDiariaSinUsar();

        whenSeReservanPaginas(1);

        thenSeRechazaPorElPlanSinHacerLaReservaDiaria();
    }

    @Test
    @DisplayName("La cuota de objetivos con IA cuenta el mes argentino y trae el límite del plan vigente")
    void cuotaObjetivosIaShouldCountTheArgentinaMonthWithThePlanLimit() {
        givenActivePlan();
        givenTresObjetivosConIaEnOctubre();

        whenSeConsultaLaCuotaDeObjetivosConIa();

        thenLaCuotaEsDeTresDeCincoConDosRestantes();
    }

    @Test
    @DisplayName("Un objetivo con IA se valida con el negocio bloqueado antes de contar")
    void validarObjetivoIaShouldLockBeforeCounting() {
        givenActivePlan();
        givenObjetivosConIaUsados(4L);

        whenSeValidaUnObjetivoConIa();

        thenSeBloqueaElNegocioAntesDeContarLosObjetivosConIa();
    }

    @Test
    @DisplayName("Con la cuota agotada rechaza con el código del límite de objetivos con IA")
    void validarObjetivoIaShouldRejectWhenTheAllowanceIsUsed() {
        givenActivePlan();
        givenObjetivosConIaUsados(5L);

        whenSeValidaUnObjetivoConIa();

        thenSeRechazaPorElLimiteDeObjetivosConIa();
    }

    // --- arrange ---
    private Plan givenActivePlan() {
        Plan plan = plan();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        return plan;
    }

    private void givenRegistrosCasiAlTope() {
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(REGISTROS_CASI_AL_TOPE);
    }

    private void givenUnaSuscripcionIniciadaElMesPasado() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setInicioEn(INSTANTE.minusMonths(1));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
    }

    private void givenUnPlanConRecetasIncluidas() {
        Plan plan = givenActivePlan();
        plan.setFunciones(List.of(FuncionPlan.RECETAS_COSTOS.name()));
    }

    private void givenUnaSuscripcionQueVenceJusto() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setFinEn(INSTANTE);
        givenFallback(suscripcion);
    }

    private void givenUnaSuscripcionCancelada() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        givenFallback(suscripcion);
    }

    private void givenUnNegocioSinSuscripcion() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenUnPlanAsignadoQueYaNoExiste() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());
    }

    private void givenUnaSuscripcionVencidaSinPlanGratuito() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.VENCIDA);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());
    }

    private void givenUnaPruebaSinHistorial() {
        givenActivePlan();
        Suscripcion prueba = suscripcion();
        prueba.setEstado(EstadoSuscripcion.PRUEBA);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba));
    }

    private void givenQueLaReservaInmediataEntra() {
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, ZonaNegocio.dia(INSTANTE),
                LIMITE_DIARIO_DE_PAGINAS, instanteEnArgentina())).thenReturn(true);
    }

    private void givenQueLaReservaInmediataNoEntra() {
        when(usoPlanRepository.registrarPaginaInmediata(any(), any(), anyInt(), any())).thenReturn(false);
    }

    private void givenUnaPruebaVencida() {
        Suscripcion prueba = suscripcion();
        prueba.setEstado(EstadoSuscripcion.PRUEBA);
        prueba.setFinEn(INSTANTE);
        givenFallback(prueba);
    }

    private void givenUnaPruebaVigente() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        pruebaVigente = PruebaPremium.nueva(NEGOCIO_ID, INSTANTE);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(pruebaVigente));
    }

    private void givenFallback(Suscripcion suscripcion) {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
    }

    private void givenUnaReservaDiariaQueEntra() {
        reservaDiaria = () -> true;
    }

    private void givenUnaReservaDiariaQueNoEntra() {
        reservaDiaria = () -> false;
    }

    private void givenUnaReservaDiariaEspiadaQueEntra() {
        reservaDiaria = mock(BooleanSupplier.class);
        when(reservaDiaria.getAsBoolean()).thenReturn(true);
    }

    private void givenUnaReservaDiariaSinUsar() {
        reservaDiaria = mock(BooleanSupplier.class);
    }

    private void givenLaCuotaMensualDePaginasLlena() {
        when(usoPlanRepository.paginasEnPeriodo(any(), any(), any())).thenReturn(PAGINAS_YA_USADAS_DEL_MES);
    }

    private void givenTresObjetivosConIaEnOctubre() {
        when(usoPlanRepository.objetivosIaEnPeriodo(NEGOCIO_ID,
                OffsetDateTime.parse("2026-10-01T03:00:00Z"), OffsetDateTime.parse("2026-11-01T03:00:00Z")))
                .thenReturn(3L);
    }

    private void givenObjetivosConIaUsados(long usados) {
        when(usoPlanRepository.objetivosIaEnPeriodo(any(), any(), any())).thenReturn(usados);
    }

    // --- act ---
    private void whenSeValidaElAltaDeUnProyecto() {
        error = catchThrowable(() -> controlPlanService.validarAltaProyecto(NEGOCIO_ID, INSTANTE));
    }

    private void whenSeValidaElAltaDeDosRegistros() {
        error = catchThrowable(() -> controlPlanService.validarAltaRegistros(NEGOCIO_ID, 2, INSTANTE));
    }

    private void whenSeValidaUnaPaginaDeIaEn(OffsetDateTime ahora) {
        error = catchThrowable(() -> controlPlanService.validarPaginasIa(NEGOCIO_ID, 1, ahora));
    }

    private void whenSeValidaLaFuncionRecetas() {
        error = catchThrowable(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE));
    }

    private void whenSeReservaUnaPaginaInmediataEnArgentina() {
        error = catchThrowable(() -> controlPlanService.reservarPaginaIaInmediata(
                NEGOCIO_ID, LIMITE_DIARIO_DE_PAGINAS, instanteEnArgentina()));
    }

    private void whenSeReservaUnaPaginaInmediata() {
        error = catchThrowable(() -> controlPlanService.reservarPaginaIaInmediata(
                NEGOCIO_ID, LIMITE_DIARIO_DE_PAGINAS, INSTANTE));
    }

    private void whenSeReservanPaginas(int paginas) {
        error = catchThrowable(() -> reservaConcedida =
                controlPlanService.reservarPaginasIa(NEGOCIO_ID, paginas, INSTANTE, reservaDiaria));
    }

    private void whenSeConsultaLaCuotaDeObjetivosConIa() {
        cuota = controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, MEDIADOS_DE_OCTUBRE);
    }

    private void whenSeValidaUnObjetivoConIa() {
        error = catchThrowable(() -> controlPlanService.validarObjetivoIa(NEGOCIO_ID, INSTANTE));
    }

    // --- assert ---
    private void thenSeBloqueaElNegocioAntesDeContar() {
        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
        orden.verify(usoPlanRepository).contarProyectos(NEGOCIO_ID);
    }

    private void thenSeRechazaPorElPlan() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void thenSeCuentanLasPaginasDeSeptiembreConElNegocioBloqueado() {
        verify(usoPlanRepository).paginasEnPeriodo(NEGOCIO_ID,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
    }

    private void thenSePermiteSinBloquearNiContar() {
        assertThat(error).isNull();
        verifyNoInteractions(usoPlanRepository);
    }

    private void thenSeRechazaPorFaltaDePremiumActivo() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class).hasMessageContaining("premium activo");
    }

    private void thenSeUsaElPlanGratuito() {
        verify(planRepository).findByCodigo(CodigoPlan.GRATUITO);
    }

    private void thenSeRechazaPorSuscripcionInexistenteSinConsultarPlanes() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");
        verifyNoInteractions(planRepository);
    }

    private void thenSeRechazaPorPlanInexistente(String mensaje) {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage(mensaje);
    }

    private void thenSeReservaLaPaginaEnElDiaArgentino() {
        assertThat(error).isNull();
        verify(usoPlanRepository).registrarPaginaInmediata(
                NEGOCIO_ID, ZonaNegocio.dia(INSTANTE), LIMITE_DIARIO_DE_PAGINAS, instanteEnArgentina());
    }

    private void thenSeRechazaPorElLimiteDiario() {
        assertThat(error)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
    }

    private void thenSeConcedeLaReserva() {
        assertThat(reservaConcedida).isTrue();
    }

    private void thenNoSeTocaElHistorialDePrueba() {
        verifyNoInteractions(pruebaPremiumRepository);
    }

    private void thenSeValidaLaReservaYSeDescuentaDeLaPruebaEnOrden() {
        assertThat(reservaConcedida).isTrue();
        var orden = inOrder(usoPlanRepository, reservaDiaria, pruebaPremiumRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(reservaDiaria).getAsBoolean();
        orden.verify(pruebaPremiumRepository).save(pruebaVigente.consumir(2));
    }

    private void thenNoSeConcedeLaReservaNiSeDescuentaDeLaPrueba() {
        assertThat(reservaConcedida).isFalse();
        verify(pruebaPremiumRepository, never()).save(any());
    }

    private void thenSeRechazaPorElPlanSinHacerLaReservaDiaria() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
        verifyNoInteractions(reservaDiaria);
    }

    private void thenLaCuotaEsDeTresDeCincoConDosRestantes() {
        assertThat(cuota).isEqualTo(new CuotaObjetivosIa(3, 5));
        assertThat(cuota.restantes()).isEqualTo(2);
    }

    private void thenSeBloqueaElNegocioAntesDeContarLosObjetivosConIa() {
        var orden = inOrder(usoPlanRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(usoPlanRepository).objetivosIaEnPeriodo(any(), any(), any());
    }

    private void thenSeRechazaPorElLimiteDeObjetivosConIa() {
        assertThat(error)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos con Sami");
    }

    // --- helpers ---
    private static OffsetDateTime instanteEnArgentina() {
        return INSTANTE.withOffsetSameInstant(ZoneOffset.ofHours(-3));
    }
}
