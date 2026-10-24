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
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.IMPORTE;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private NegocioService negocioService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private RenovacionPremiumService renovacionPremiumService;

    @Mock
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @Mock
    private SincronizarRenovacion sincronizarRenovacion;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    private ContratarPremium contratarPremium;

    @BeforeEach
    void setUp() {
        contratarPremium = new ContratarPremium(negocioService, usuarioService, renovacionPremiumService,
                pasarelaSuscripcionesPort, sincronizarRenovacion,
                new SuscripcionService(planRepository, suscripcionRepository), renovacionPremiumRepository,
                usoPlanRepository, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(usuarioService.obtenerActivo(USUARIO_ID)).thenReturn(Usuario.builder().email("ana@example.test").build());
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("No prepara una contratación con precio o moneda pendientes o inválidos")
    void executeShouldRejectInvalidCommercialConditions(int caso) {
        Plan plan = premium();
        switch (caso) {
            case 0 -> plan.setConfigurado(false);
            case 1 -> plan.setConfigurado(null);
            case 2 -> plan.setPrecioMensual(null);
            case 3 -> plan.setPrecioMensual(BigDecimal.ZERO);
            case 4 -> plan.setPrecioMensual(new BigDecimal("-1"));
            case 5 -> plan.setMoneda("USD");
            default -> throw new IllegalArgumentException();
        }
        givenCatalogo(plan);

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(suscripcionRepository, renovacionPremiumRepository);
        verify(pasarelaSuscripcionesPort, never()).crear(any(), anyString());
    }

    @Test
    @DisplayName("No cobra otra vez a un negocio con Premium sin vencimiento")
    void executeShouldRejectIndefinitePremium() {
        givenCatalogo(premium());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion(PREMIUM_ID)));

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un Premium vencido puede volver a contratar sin conservar fechas anteriores")
    void executeShouldAllowExpiredPremium() {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        givenContratoCreado();

        contratarPremium.execute(USUARIO_ID);

        assertThat(thenRenovacionPreparada().getPrimerCobroEn()).isEqualTo(AHORA);
        verify(pasarelaSuscripcionesPort).crear(any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 999_999, 1_000_000, 353_444_000, 999_999_999})
    @DisplayName("El primer cobro usa segundos completos sin anticipar el fin de la prueba")
    void executeShouldNeverRoundBeforeTrialEnd(int nanos) {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA.plusDays(4).plusNanos(nanos));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        givenContratoCreado();

        contratarPremium.execute(USUARIO_ID);

        RenovacionPremium preparada = thenRenovacionPreparada();
        assertThat(preparada.getPrimerCobroEn()).isEqualTo(AHORA.plusDays(4).plusSeconds(1));
        assertThat(preparada.getPrimerCobroEn()).isAfter(suscripcion.getFinEn());
    }

    @Test
    @DisplayName("Un contrato cancelado permite una contratación nueva conservando su historial")
    void executeShouldAllowNewContractAfterCancellation() {
        RenovacionPremium anterior = renovacion();
        anterior.setEstado("CANCELADA");
        when(renovacionPremiumService.consultar(NEGOCIO_ID))
                .thenReturn(Optional.of(anterior), Optional.of(renovacion()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion(GRATUITO_ID)));
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        contratarPremium.execute(USUARIO_ID);

        assertThat(thenRenovacionPreparada().getId()).isNotEqualTo(anterior.getId());
    }

    @Test
    @DisplayName("Una renovación en curso no se vuelve a crear: se sincroniza con la pasarela")
    void executeShouldSyncOpenContractInsteadOfCreatingAnother() {
        RenovacionPremium abierta = renovacion();
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.of(abierta));

        contratarPremium.execute(USUARIO_ID);

        verify(sincronizarRenovacion).execute(abierta.getId());
        verify(pasarelaSuscripcionesPort, never()).crear(any(), anyString());
        verifyNoInteractions(renovacionPremiumRepository);
    }

    @Test
    @DisplayName("Sin suscripción no se inicia un cobro")
    void executeShouldRejectMissingSubscription() {
        givenCatalogo(premium());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sin catálogo no se inventa un precio")
    void executeShouldRejectMissingCatalog() {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratarPremium.execute(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
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
    private RenovacionPremium thenRenovacionPreparada() {
        ArgumentCaptor<RenovacionPremium> renovacion = ArgumentCaptor.forClass(RenovacionPremium.class);
        verify(renovacionPremiumRepository).save(renovacion.capture());
        assertThat(renovacion.getValue().getEstado()).isEqualTo("PREPARANDO");
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        return renovacion.getValue();
    }
}
