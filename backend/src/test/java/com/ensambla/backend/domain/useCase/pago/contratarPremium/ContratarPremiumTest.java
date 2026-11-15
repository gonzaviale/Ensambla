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
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.IMPORTE;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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

    @ParameterizedTest
    @MethodSource("planesConCondicionesComercialesInvalidas")
    @DisplayName("No prepara una contratación con precio o moneda pendientes o inválidos")
    void executeShouldRejectInvalidCommercialConditions(Plan plan) {
        givenCatalogo(plan);

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(BusinessRuleException.class);

        thenNoSeIniciaNingunCobro();
    }

    @Test
    @DisplayName("No cobra otra vez a un negocio con Premium sin vencimiento")
    void executeShouldRejectIndefinitePremium() {
        givenCatalogo(premium());
        givenSuscripcion(suscripcion(PREMIUM_ID));

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un Premium vencido puede volver a contratar sin conservar fechas anteriores")
    void executeShouldAllowExpiredPremium() {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA);
        givenSuscripcion(suscripcion);
        givenContratoCreado();

        contratarPremium.execute(USUARIO_ID);

        assertThat(thenRenovacionPreparada().getPrimerCobroEn()).isEqualTo(AHORA);
        thenSeCreaElContratoEnLaPasarela();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 999_999, 1_000_000, 353_444_000, 999_999_999})
    @DisplayName("El primer cobro usa segundos completos sin anticipar el fin de la prueba")
    void executeShouldNeverRoundBeforeTrialEnd(int nanos) {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA.plusDays(4).plusNanos(nanos));
        givenSuscripcion(suscripcion);
        givenContratoCreado();

        contratarPremium.execute(USUARIO_ID);

        RenovacionPremium preparada = thenRenovacionPreparada();
        assertThat(preparada.getPrimerCobroEn()).isEqualTo(AHORA.plusDays(4).plusSeconds(1));
        assertThat(preparada.getPrimerCobroEn()).isAfter(suscripcion.getFinEn());
    }

    @Test
    @DisplayName("Un contrato cancelado permite una contratación nueva conservando su historial")
    void executeShouldAllowNewContractAfterCancellation() {
        RenovacionPremium anterior = givenRenovacionCancelada();
        givenCatalogoConPlanGratuitoActual();

        contratarPremium.execute(USUARIO_ID);

        assertThat(thenRenovacionPreparada().getId()).isNotEqualTo(anterior.getId());
    }

    @Test
    @DisplayName("Una renovación en curso no se vuelve a crear: se sincroniza con la pasarela")
    void executeShouldSyncOpenContractInsteadOfCreatingAnother() {
        RenovacionPremium abierta = renovacion();
        givenRenovacionEnCurso(abierta);

        contratarPremium.execute(USUARIO_ID);

        thenSeSincronizaConLaPasarela(abierta);
        thenNoSeCreaContratoEnLaPasarela();
        thenNoSePreparaOtraRenovacion();
    }

    @Test
    @DisplayName("Sin suscripción no se inicia un cobro")
    void executeShouldRejectMissingSubscription() {
        givenCatalogo(premium());
        givenSinSuscripcion();

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sin catálogo no se inventa un precio")
    void executeShouldRejectMissingCatalog() {
        givenSinCatalogo();

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    static Stream<Arguments> planesConCondicionesComercialesInvalidas() {
        return Stream.of(
                planInvalido("sin marcar como configurado", plan -> plan.setConfigurado(false)),
                planInvalido("con el configurado sin informar", plan -> plan.setConfigurado(null)),
                planInvalido("sin precio", plan -> plan.setPrecioMensual(null)),
                planInvalido("con precio cero", plan -> plan.setPrecioMensual(BigDecimal.ZERO)),
                planInvalido("con precio negativo", plan -> plan.setPrecioMensual(new BigDecimal("-1"))),
                planInvalido("en una moneda que no es pesos", plan -> plan.setMoneda("USD")));
    }

    private static Arguments planInvalido(String descripcion, Consumer<Plan> deformacion) {
        Plan plan = premium();
        deformacion.accept(plan);
        return Arguments.of(Named.of(descripcion, plan));
    }

    // --- arrange ---
    private void givenSuscripcion(Suscripcion suscripcion) {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
    }

    private void givenSinSuscripcion() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenRenovacionEnCurso(RenovacionPremium renovacion) {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.of(renovacion));
    }

    private void givenSinCatalogo() {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());
    }

    private RenovacionPremium givenRenovacionCancelada() {
        RenovacionPremium anterior = renovacion();
        anterior.setEstado("CANCELADA");
        when(renovacionPremiumService.consultar(NEGOCIO_ID))
                .thenReturn(Optional.of(anterior), Optional.of(renovacion()));
        return anterior;
    }

    private void givenCatalogoConPlanGratuitoActual() {
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        givenSuscripcion(suscripcion(GRATUITO_ID));
        givenContratoCreado();
    }

    private void givenCatalogo(Plan plan) {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty(), Optional.of(renovacion()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(plan));
    }

    private void givenContratoCreado() {
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Plan premium() {
        return Plan.builder().id(PREMIUM_ID).codigo(CodigoPlan.PREMIUM).configurado(true)
                .limitesConfigurados(true).precioMensual(IMPORTE).moneda("ARS").build();
    }

    private static Suscripcion suscripcion(PlanId planId) {
        return Suscripcion.builder().negocioId(NEGOCIO_ID).planId(planId).estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA.minusDays(1)).build();
    }

    // --- assert ---
    private void thenSeCreaElContratoEnLaPasarela() {
        verify(pasarelaSuscripcionesPort).crear(any(), anyString());
    }

    private void thenNoSeCreaContratoEnLaPasarela() {
        verify(pasarelaSuscripcionesPort, never()).crear(any(), anyString());
    }

    private void thenNoSePreparaOtraRenovacion() {
        verifyNoInteractions(renovacionPremiumRepository);
    }

    private void thenSeSincronizaConLaPasarela(RenovacionPremium renovacion) {
        verify(renovacionPremiumService).sincronizar(renovacion.getId(), AHORA);
    }

    private void thenNoSeIniciaNingunCobro() {
        verifyNoInteractions(suscripcionRepository, renovacionPremiumRepository);
        verify(pasarelaSuscripcionesPort, never()).crear(any(), anyString());
    }

    private RenovacionPremium thenRenovacionPreparada() {
        ArgumentCaptor<RenovacionPremium> renovacion = ArgumentCaptor.forClass(RenovacionPremium.class);
        verify(renovacionPremiumRepository).save(renovacion.capture());
        assertThat(renovacion.getValue().getEstado()).isEqualTo("PREPARANDO");
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        return renovacion.getValue();
    }
}
