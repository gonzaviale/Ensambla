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
    void bloqueaElNegocioAntesDeContarYAutorizarUnSistema() {
        dadoActivePlan();

        cuandoSeValidaElAltaDeUnProyecto();

        entoncesSeBloqueaElNegocioAntesDeContar();
    }

    @Test
    @DisplayName("Rechaza todas las filas del lote cuando no entran en la cuota")
    void rechazaTodasLasFilasDelLoteCuandoNoEntranEnLaCuota() {
        dadoActivePlan();
        dadoRegistrosCasiAlTope();

        cuandoSeValidaElAltaDeDosRegistros();

        entoncesSeRechazaPorElPlan();
    }

    @Test
    @DisplayName("Calcula el mes por el día argentino aunque en UTC ya sea el mes siguiente")
    void calculaElMesPorElDiaArgentinoAunqueEnUtcYaSeaElMesSiguiente() {
        dadoUnaSuscripcionIniciadaElMesPasado();

        cuandoSeValidaUnaPaginaDeIaEn(MADRUGADA_UTC_DEL_PRIMERO_DE_OCTUBRE);

        entoncesSeCuentanLasPaginasDeSeptiembreConElNegocioBloqueado();
    }

    @Test
    @DisplayName("Una función incluida se permite sin bloquear ni contar las altas")
    void unaFuncionIncluidaSePermiteSinBloquearNiContarLasAltas() {
        dadoUnPlanConRecetasIncluidas();

        cuandoSeValidaLaFuncionRecetas();

        entoncesSePermiteSinBloquearNiContar();
    }

    @Test
    @DisplayName("La suscripción vencida usa el gratuito aunque todavía figure activa en persistencia")
    void laSuscripcionVencidaUsaElGratuitoAunqueTodaviaFigureActivaEnPersistencia() {
        dadoUnaSuscripcionQueVenceJusto();

        cuandoSeValidaLaFuncionRecetas();

        entoncesSeRechazaPorFaltaDePremiumActivo();
    }

    @Test
    @DisplayName("Una suscripción cancelada conserva los límites de alta del gratuito")
    void unaSuscripcionCanceladaConservaLosLimitesDeAltaDelGratuito() {
        dadoUnaSuscripcionCancelada();

        cuandoSeValidaElAltaDeUnProyecto();

        entoncesSeUsaElPlanGratuito();
    }

    @Test
    @DisplayName("Un negocio sin suscripción no recibe capacidades por defecto")
    void unNegocioSinSuscripcionNoRecibeCapacidadesPorDefecto() {
        dadoUnNegocioSinSuscripcion();

        cuandoSeValidaLaFuncionRecetas();

        entoncesSeRechazaPorSuscripcionInexistenteSinConsultarPlanes();
    }

    @Test
    @DisplayName("No habilita un plan referido que ya no existe")
    void noHabilitaUnPlanReferidoQueYaNoExiste() {
        dadoUnPlanAsignadoQueYaNoExiste();

        cuandoSeValidaLaFuncionRecetas();

        entoncesSeRechazaPorPlanInexistente("Plan no encontrado");
    }

    @Test
    @DisplayName("Una suscripción vencida no habilita funciones si falta el catálogo gratuito")
    void unaSuscripcionVencidaNoHabilitaFuncionesSiFaltaElCatalogoGratuito() {
        dadoUnaSuscripcionVencidaSinPlanGratuito();

        cuandoSeValidaLaFuncionRecetas();

        entoncesSeRechazaPorPlanInexistente("Plan gratuito no encontrado");
    }

    @Test
    @DisplayName("Una prueba sin historial persistido falla sin autorizar páginas")
    void unaPruebaSinHistorialPersistidoFallaSinAutorizarPaginas() {
        dadoUnaPruebaSinHistorial();

        cuandoSeValidaUnaPaginaDeIaEn(INSTANTE);

        entoncesSeRechazaPorPlanInexistente("Prueba no encontrada");
    }

    @Test
    @DisplayName("Una foto inmediata valida el mes, reserva en el día argentino con el límite diario y no lanza si entra")
    void unaFotoInmediataValidaElMesReservaEnElDiaArgentinoConElLimiteDiarioYNoLanza() {
        dadoActivePlan();
        dadoQueLaReservaInmediataEntra();

        cuandoSeReservaUnaPaginaInmediataEnArgentina();

        entoncesSeReservaLaPaginaEnElDiaArgentino();
    }

    @Test
    @DisplayName("Una foto inmediata con el límite diario agotado responde la restricción del día")
    void unaFotoInmediataConElLimiteDiarioAgotadoRespondeLaRestriccionDelDia() {
        dadoActivePlan();
        dadoQueLaReservaInmediataNoEntra();

        cuandoSeReservaUnaPaginaInmediata();

        entoncesSeRechazaPorElLimiteDiario();
    }

    @Test
    @DisplayName("La reserva de páginas tampoco admite un negocio sin suscripción")
    void laReservaDePaginasTampocoAdmiteUnNegocioSinSuscripcion() {
        dadoUnaReservaDiariaQueEntra();

        cuandoSeReservanPaginas(1);

        entoncesSeRechazaPorPlanInexistente("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Una prueba vencida usa la cuota mensual gratuita")
    void unaPruebaVencidaUsaLaCuotaMensualGratuita() {
        dadoUnaPruebaVencida();
        dadoUnaReservaDiariaQueEntra();

        cuandoSeReservanPaginas(1);

        entoncesSeConcedeLaReserva();
    }

    @Test
    @DisplayName("Una prueba vencida no incrementa el historial de prueba")
    void unaPruebaVencidaNoIncrementaElHistorialDePrueba() {
        dadoUnaPruebaVencida();
        dadoUnaReservaDiariaQueEntra();

        cuandoSeReservanPaginas(1);

        entoncesNoSeTocaElHistorialDePrueba();
    }

    @Test
    @DisplayName("Valida la cuota de la prueba antes de la reserva diaria y, si entra, la descuenta de la prueba")
    void validaLaCuotaDeLaPruebaAntesDeLaReservaDiariaYSiEntraLaDescuentaDeLaPrueba() {
        dadoUnaPruebaVigente();
        dadoUnaReservaDiariaEspiadaQueEntra();

        cuandoSeReservanPaginas(2);

        entoncesSeValidaLaReservaYSeDescuentaDeLaPruebaEnOrden();
    }

    @Test
    @DisplayName("Una reserva diaria que no entra no descuenta páginas de la prueba")
    void unaReservaDiariaQueNoEntraNoDescuentaPaginasDeLaPrueba() {
        dadoUnaPruebaVigente();
        dadoUnaReservaDiariaQueNoEntra();

        cuandoSeReservanPaginas(2);

        entoncesNoSeConcedeLaReservaNiSeDescuentaDeLaPrueba();
    }

    @Test
    @DisplayName("Un rechazo de la cuota mensual no llega a hacer la reserva diaria")
    void unRechazoDeLaCuotaMensualNoLlegaAHacerLaReservaDiaria() {
        dadoActivePlan();
        dadoLaCuotaMensualDePaginasLlena();
        dadoUnaReservaDiariaSinUsar();

        cuandoSeReservanPaginas(1);

        entoncesSeRechazaPorElPlanSinHacerLaReservaDiaria();
    }

    @Test
    @DisplayName("La cuota de objetivos con IA cuenta el mes argentino y trae el límite del plan vigente")
    void laCuotaDeObjetivosConIaCuentaElMesArgentinoYTraeElLimiteDelPlanVigente() {
        dadoActivePlan();
        dadoTresObjetivosConIaEnOctubre();

        cuandoSeConsultaLaCuotaDeObjetivosConIa();

        entoncesLaCuotaEsDeTresDeCincoConDosRestantes();
    }

    @Test
    @DisplayName("Un objetivo con IA se valida con el negocio bloqueado antes de contar")
    void unObjetivoConIaSeValidaConElNegocioBloqueadoAntesDeContar() {
        dadoActivePlan();
        dadoObjetivosConIaUsados(4L);

        cuandoSeValidaUnObjetivoConIa();

        entoncesSeBloqueaElNegocioAntesDeContarLosObjetivosConIa();
    }

    @Test
    @DisplayName("Con la cuota agotada rechaza con el código del límite de objetivos con IA")
    void conLaCuotaAgotadaRechazaConElCodigoDelLimiteDeObjetivosConIa() {
        dadoActivePlan();
        dadoObjetivosConIaUsados(5L);

        cuandoSeValidaUnObjetivoConIa();

        entoncesSeRechazaPorElLimiteDeObjetivosConIa();
    }

    // --- arrange ---
    private Plan dadoActivePlan() {
        Plan plan = plan();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        return plan;
    }

    private void dadoRegistrosCasiAlTope() {
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(REGISTROS_CASI_AL_TOPE);
    }

    private void dadoUnaSuscripcionIniciadaElMesPasado() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setInicioEn(INSTANTE.minusMonths(1));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
    }

    private void dadoUnPlanConRecetasIncluidas() {
        Plan plan = dadoActivePlan();
        plan.setFunciones(List.of(FuncionPlan.RECETAS_COSTOS.name()));
    }

    private void dadoUnaSuscripcionQueVenceJusto() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setFinEn(INSTANTE);
        dadoFallback(suscripcion);
    }

    private void dadoUnaSuscripcionCancelada() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        dadoFallback(suscripcion);
    }

    private void dadoUnNegocioSinSuscripcion() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnPlanAsignadoQueYaNoExiste() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaSuscripcionVencidaSinPlanGratuito() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.VENCIDA);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());
    }

    private void dadoUnaPruebaSinHistorial() {
        dadoActivePlan();
        Suscripcion prueba = suscripcion();
        prueba.setEstado(EstadoSuscripcion.PRUEBA);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba));
    }

    private void dadoQueLaReservaInmediataEntra() {
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, ZonaNegocio.dia(INSTANTE),
                LIMITE_DIARIO_DE_PAGINAS, instanteEnArgentina())).thenReturn(true);
    }

    private void dadoQueLaReservaInmediataNoEntra() {
        when(usoPlanRepository.registrarPaginaInmediata(any(), any(), anyInt(), any())).thenReturn(false);
    }

    private void dadoUnaPruebaVencida() {
        Suscripcion prueba = suscripcion();
        prueba.setEstado(EstadoSuscripcion.PRUEBA);
        prueba.setFinEn(INSTANTE);
        dadoFallback(prueba);
    }

    private void dadoUnaPruebaVigente() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        pruebaVigente = PruebaPremium.nueva(NEGOCIO_ID, INSTANTE);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(pruebaVigente));
    }

    private void dadoFallback(Suscripcion suscripcion) {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
    }

    private void dadoUnaReservaDiariaQueEntra() {
        reservaDiaria = () -> true;
    }

    private void dadoUnaReservaDiariaQueNoEntra() {
        reservaDiaria = () -> false;
    }

    private void dadoUnaReservaDiariaEspiadaQueEntra() {
        reservaDiaria = mock(BooleanSupplier.class);
        when(reservaDiaria.getAsBoolean()).thenReturn(true);
    }

    private void dadoUnaReservaDiariaSinUsar() {
        reservaDiaria = mock(BooleanSupplier.class);
    }

    private void dadoLaCuotaMensualDePaginasLlena() {
        when(usoPlanRepository.paginasEnPeriodo(any(), any(), any())).thenReturn(PAGINAS_YA_USADAS_DEL_MES);
    }

    private void dadoTresObjetivosConIaEnOctubre() {
        when(usoPlanRepository.objetivosIaEnPeriodo(NEGOCIO_ID,
                OffsetDateTime.parse("2026-10-01T03:00:00Z"), OffsetDateTime.parse("2026-11-01T03:00:00Z")))
                .thenReturn(3L);
    }

    private void dadoObjetivosConIaUsados(long usados) {
        when(usoPlanRepository.objetivosIaEnPeriodo(any(), any(), any())).thenReturn(usados);
    }

    // --- act ---
    private void cuandoSeValidaElAltaDeUnProyecto() {
        error = catchThrowable(() -> controlPlanService.validarAltaProyecto(NEGOCIO_ID, INSTANTE));
    }

    private void cuandoSeValidaElAltaDeDosRegistros() {
        error = catchThrowable(() -> controlPlanService.validarAltaRegistros(NEGOCIO_ID, 2, INSTANTE));
    }

    private void cuandoSeValidaUnaPaginaDeIaEn(OffsetDateTime ahora) {
        error = catchThrowable(() -> controlPlanService.validarPaginasIa(NEGOCIO_ID, 1, ahora));
    }

    private void cuandoSeValidaLaFuncionRecetas() {
        error = catchThrowable(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE));
    }

    private void cuandoSeReservaUnaPaginaInmediataEnArgentina() {
        error = catchThrowable(() -> controlPlanService.reservarPaginaIaInmediata(
                NEGOCIO_ID, LIMITE_DIARIO_DE_PAGINAS, instanteEnArgentina()));
    }

    private void cuandoSeReservaUnaPaginaInmediata() {
        error = catchThrowable(() -> controlPlanService.reservarPaginaIaInmediata(
                NEGOCIO_ID, LIMITE_DIARIO_DE_PAGINAS, INSTANTE));
    }

    private void cuandoSeReservanPaginas(int paginas) {
        error = catchThrowable(() -> reservaConcedida =
                controlPlanService.reservarPaginasIa(NEGOCIO_ID, paginas, INSTANTE, reservaDiaria));
    }

    private void cuandoSeConsultaLaCuotaDeObjetivosConIa() {
        cuota = controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, MEDIADOS_DE_OCTUBRE);
    }

    private void cuandoSeValidaUnObjetivoConIa() {
        error = catchThrowable(() -> controlPlanService.validarObjetivoIa(NEGOCIO_ID, INSTANTE));
    }

    // --- assert ---
    private void entoncesSeBloqueaElNegocioAntesDeContar() {
        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
        orden.verify(usoPlanRepository).contarProyectos(NEGOCIO_ID);
    }

    private void entoncesSeRechazaPorElPlan() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesSeCuentanLasPaginasDeSeptiembreConElNegocioBloqueado() {
        verify(usoPlanRepository).paginasEnPeriodo(NEGOCIO_ID,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
    }

    private void entoncesSePermiteSinBloquearNiContar() {
        assertThat(error).isNull();
        verifyNoInteractions(usoPlanRepository);
    }

    private void entoncesSeRechazaPorFaltaDePremiumActivo() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class).hasMessageContaining("premium activo");
    }

    private void entoncesSeUsaElPlanGratuito() {
        verify(planRepository).findByCodigo(CodigoPlan.GRATUITO);
    }

    private void entoncesSeRechazaPorSuscripcionInexistenteSinConsultarPlanes() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");
        verifyNoInteractions(planRepository);
    }

    private void entoncesSeRechazaPorPlanInexistente(String mensaje) {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage(mensaje);
    }

    private void entoncesSeReservaLaPaginaEnElDiaArgentino() {
        assertThat(error).isNull();
        verify(usoPlanRepository).registrarPaginaInmediata(
                NEGOCIO_ID, ZonaNegocio.dia(INSTANTE), LIMITE_DIARIO_DE_PAGINAS, instanteEnArgentina());
    }

    private void entoncesSeRechazaPorElLimiteDiario() {
        assertThat(error)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
    }

    private void entoncesSeConcedeLaReserva() {
        assertThat(reservaConcedida).isTrue();
    }

    private void entoncesNoSeTocaElHistorialDePrueba() {
        verifyNoInteractions(pruebaPremiumRepository);
    }

    private void entoncesSeValidaLaReservaYSeDescuentaDeLaPruebaEnOrden() {
        assertThat(reservaConcedida).isTrue();
        var orden = inOrder(usoPlanRepository, reservaDiaria, pruebaPremiumRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(reservaDiaria).getAsBoolean();
        orden.verify(pruebaPremiumRepository).save(pruebaVigente.consumir(2));
    }

    private void entoncesNoSeConcedeLaReservaNiSeDescuentaDeLaPrueba() {
        assertThat(reservaConcedida).isFalse();
        verify(pruebaPremiumRepository, never()).save(any());
    }

    private void entoncesSeRechazaPorElPlanSinHacerLaReservaDiaria() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
        verifyNoInteractions(reservaDiaria);
    }

    private void entoncesLaCuotaEsDeTresDeCincoConDosRestantes() {
        assertThat(cuota).isEqualTo(new CuotaObjetivosIa(3, 5));
        assertThat(cuota.restantes()).isEqualTo(2);
    }

    private void entoncesSeBloqueaElNegocioAntesDeContarLosObjetivosConIa() {
        var orden = inOrder(usoPlanRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(usoPlanRepository).objetivosIaEnPeriodo(any(), any(), any());
    }

    private void entoncesSeRechazaPorElLimiteDeObjetivosConIa() {
        assertThat(error)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos con Sami");
    }

    // --- helpers ---
    private static OffsetDateTime instanteEnArgentina() {
        return INSTANTE.withOffsetSameInstant(ZoneOffset.ofHours(-3));
    }
}
