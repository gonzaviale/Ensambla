package com.ensambla.backend.domain.useCase.pago.contratarPremium;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.IMPORTE;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratarPremiumTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-0000-0000-000000000009");
    private static final PlanId PREMIUM_ID = PlanId.de("b0000000-0000-0000-0000-000000000001");
    private static final PlanId GRATUITO_ID = PlanId.de("b0000000-0000-0000-0000-000000000002");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RenovacionPremiumService renovacionPremiumService;

    @Mock
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private PlanRepository planRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    private ContratarPremium contratarPremium;

    private Throwable rechazo;

    private RenovacionPremium renovacionCancelada;

    private RenovacionPremium renovacionEnCurso;

    private OffsetDateTime finDeLaPrueba;

    @BeforeEach
    void setUp() {
        contratarPremium = new ContratarPremium(negocioRepository, usuarioRepository, renovacionPremiumService,
                pasarelaSuscripcionesPort,
                suscripcionRepository, planRepository, renovacionPremiumRepository,
                usoPlanRepository, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(usuarioRepository.obtenerActivo(USUARIO_ID))
                .thenReturn(Usuario.builder().email("ana@example.test").build());
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("No se puede contratar Premium sin marcar el plan como configurado")
    void noSePuedeContratarPremiumSinMarcarElPlanComoConfigurado() {
        dadoUnCatalogoConPremium(premiumSinMarcarComoConfigurado());

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaSinIniciarNingunCobro();
    }

    @Test
    @DisplayName("No se puede contratar Premium si no se informó si el plan está configurado")
    void noSePuedeContratarPremiumConElConfiguradoSinInformar() {
        dadoUnCatalogoConPremium(premiumConElConfiguradoSinInformar());

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaSinIniciarNingunCobro();
    }

    @Test
    @DisplayName("No se puede contratar Premium sin precio configurado")
    void noSePuedeContratarPremiumSinPrecioConfigurado() {
        dadoUnCatalogoConPremium(premiumSinPrecio());

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaSinIniciarNingunCobro();
    }

    @Test
    @DisplayName("No se puede contratar Premium con precio cero")
    void noSePuedeContratarPremiumConPrecioCero() {
        dadoUnCatalogoConPremium(premiumConPrecio(BigDecimal.ZERO));

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaSinIniciarNingunCobro();
    }

    @Test
    @DisplayName("No se puede contratar Premium con precio negativo")
    void noSePuedeContratarPremiumConPrecioNegativo() {
        dadoUnCatalogoConPremium(premiumConPrecio(new BigDecimal("-1")));

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaSinIniciarNingunCobro();
    }

    @Test
    @DisplayName("Solo se puede contratar Premium en pesos")
    void soloSePuedeContratarPremiumEnPesos() {
        dadoUnCatalogoConPremium(premiumEnMoneda("USD"));

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaSinIniciarNingunCobro();
    }

    @Test
    @DisplayName("Un negocio con Premium sin vencimiento no paga otra vez")
    void unNegocioConPremiumSinVencimientoNoPagaOtraVez() {
        dadoUnNegocioConPremiumSinVencimiento();

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Un Premium vencido puede volver a contratarse sin conservar las fechas anteriores")
    void unPremiumVencidoPuedeVolverAContratarSinConservarFechasAnteriores() {
        dadoUnNegocioConPremiumVencidoAhora();

        cuandoSeContrataPremium();

        entoncesElPrimerCobroEsAhoraYSeCreaElContratoEnLaPasarela();
    }

    @ParameterizedTest(name = "con {0} nanosegundos pasado el segundo")
    @ValueSource(ints = {1, 999_999, 1_000_000, 353_444_000, 999_999_999})
    @DisplayName("El primer cobro nunca se adelanta al fin de la prueba")
    void elPrimerCobroNuncaSeAdelantaAlFinDeLaPrueba(int nanos) {
        dadoUnaPruebaQueTerminaConNanosegundosSobrantes(nanos);

        cuandoSeContrataPremium();

        entoncesElPrimerCobroEsElSegundoCompletoPosteriorAlFinDeLaPrueba();
    }

    @Test
    @DisplayName("Un contrato cancelado permite contratar de nuevo conservando su historial")
    void unContratoCanceladoPermiteContratarDeNuevoConservandoSuHistorial() {
        dadoUnNegocioConUnContratoCancelado();

        cuandoSeContrataPremium();

        entoncesSePreparaUnaRenovacionDistintaDeLaCancelada();
    }

    @Test
    @DisplayName("Una renovación en curso se sincroniza con la pasarela en vez de crear otra")
    void unaRenovacionEnCursoSeSincronizaConLaPasarelaEnVezDeCrearOtra() {
        dadoUnNegocioConUnaRenovacionEnCurso();

        cuandoSeContrataPremium();

        entoncesSeSincronizaLaRenovacionEnCursoSinCrearOtra();
    }

    @Test
    @DisplayName("Sin suscripción no se inicia un cobro")
    void sinSuscripcionNoSeIniciaUnCobro() {
        dadoUnNegocioSinSuscripcion();

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaPorRecursoInexistente();
    }

    @Test
    @DisplayName("Sin catálogo no se inventa un precio")
    void sinCatalogoNoSeInventaUnPrecio() {
        dadoQueNoHayCatalogo();

        cuandoSeIntentaContratarPremium();

        entoncesSeRechazaPorRecursoInexistente();
    }

    private void dadoUnCatalogoConPremium(Plan plan) {
        registrarCatalogo(plan);
    }

    private void dadoUnNegocioConPremiumSinVencimiento() {
        registrarCatalogo(premium());
        registrarSuscripcion(suscripcion(PREMIUM_ID));
    }

    private void dadoUnNegocioConPremiumVencidoAhora() {
        registrarCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA);
        registrarSuscripcion(suscripcion);
        registrarQueGuardarDevuelveLoRecibido();
    }

    private void dadoUnaPruebaQueTerminaConNanosegundosSobrantes(int nanos) {
        finDeLaPrueba = AHORA.plusDays(4).plusNanos(nanos);
        registrarCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(finDeLaPrueba);
        registrarSuscripcion(suscripcion);
        registrarQueGuardarDevuelveLoRecibido();
    }

    private void dadoUnNegocioConUnContratoCancelado() {
        renovacionCancelada = renovacion();
        renovacionCancelada.setEstado("CANCELADA");
        when(renovacionPremiumService.consultar(NEGOCIO_ID))
                .thenReturn(Optional.of(renovacionCancelada), Optional.of(renovacion()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        registrarSuscripcion(suscripcion(GRATUITO_ID));
        registrarQueGuardarDevuelveLoRecibido();
    }

    private void dadoUnNegocioConUnaRenovacionEnCurso() {
        renovacionEnCurso = renovacion();
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.of(renovacionEnCurso));
    }

    private void dadoUnNegocioSinSuscripcion() {
        registrarCatalogo(premium());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoQueNoHayCatalogo() {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());
    }

    private void cuandoSeContrataPremium() {
        contratarPremium.execute(USUARIO_ID);
    }

    private void cuandoSeIntentaContratarPremium() {
        rechazo = catchThrowable(() -> contratarPremium.execute(USUARIO_ID));
    }

    private void entoncesSeRechazaSinIniciarNingunCobro() {
        entoncesSeRechazaPorReglaDeNegocio();
        verifyNoInteractions(suscripcionRepository, renovacionPremiumRepository);
        verify(pasarelaSuscripcionesPort, never()).crear(any(), anyString());
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaPorRecursoInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesElPrimerCobroEsAhoraYSeCreaElContratoEnLaPasarela() {
        assertThat(renovacionPreparada().getPrimerCobroEn()).isEqualTo(AHORA);
        verify(pasarelaSuscripcionesPort).crear(any(), anyString());
    }

    private void entoncesElPrimerCobroEsElSegundoCompletoPosteriorAlFinDeLaPrueba() {
        OffsetDateTime primerCobro = renovacionPreparada().getPrimerCobroEn();
        assertThat(primerCobro).isEqualTo(AHORA.plusDays(4).plusSeconds(1));
        assertThat(primerCobro).isAfter(finDeLaPrueba);
    }

    private void entoncesSePreparaUnaRenovacionDistintaDeLaCancelada() {
        assertThat(renovacionPreparada().getId()).isNotEqualTo(renovacionCancelada.getId());
    }

    private void entoncesSeSincronizaLaRenovacionEnCursoSinCrearOtra() {
        verify(renovacionPremiumService).sincronizar(renovacionEnCurso.getId(), AHORA);
        verify(pasarelaSuscripcionesPort, never()).crear(any(), anyString());
        verifyNoInteractions(renovacionPremiumRepository);
    }

    private RenovacionPremium renovacionPreparada() {
        ArgumentCaptor<RenovacionPremium> renovacion = ArgumentCaptor.forClass(RenovacionPremium.class);
        verify(renovacionPremiumRepository).save(renovacion.capture());
        assertThat(renovacion.getValue().getEstado()).isEqualTo("PREPARANDO");
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        return renovacion.getValue();
    }

    private void registrarSuscripcion(Suscripcion suscripcion) {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
    }

    private void registrarCatalogo(Plan plan) {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty(), Optional.of(renovacion()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(plan));
    }

    private void registrarQueGuardarDevuelveLoRecibido() {
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Plan premium() {
        return Plan.builder().id(PREMIUM_ID).codigo(CodigoPlan.PREMIUM).configurado(true)
                .limitesConfigurados(true).precioMensual(IMPORTE).moneda("ARS").build();
    }

    private static Plan premiumSinMarcarComoConfigurado() {
        Plan plan = premium();
        plan.setConfigurado(false);
        return plan;
    }

    private static Plan premiumConElConfiguradoSinInformar() {
        Plan plan = premium();
        plan.setConfigurado(null);
        return plan;
    }

    private static Plan premiumSinPrecio() {
        return premiumConPrecio(null);
    }

    private static Plan premiumConPrecio(BigDecimal precio) {
        Plan plan = premium();
        plan.setPrecioMensual(precio);
        return plan;
    }

    private static Plan premiumEnMoneda(String moneda) {
        Plan plan = premium();
        plan.setMoneda(moneda);
        return plan;
    }

    private static Suscripcion suscripcion(PlanId planId) {
        return Suscripcion.builder().negocioId(NEGOCIO_ID).planId(planId).estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA.minusDays(1)).build();
    }
}
