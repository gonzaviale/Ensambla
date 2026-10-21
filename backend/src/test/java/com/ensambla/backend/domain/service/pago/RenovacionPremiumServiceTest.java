package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.IMPORTE;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.contrato;
import static com.ensambla.backend.support.PagoTestData.pago;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenovacionPremiumServiceTest {

    private static final UUID PREMIUM_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID GRATUITO_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private PagoPremiumRepository pagoPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private RenovacionPremiumService renovacionPremiumService;

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("No prepara una contratación con precio o moneda pendientes o inválidos")
    void prepararShouldRejectInvalidCommercialConditions(int caso) {
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

        assertThatThrownBy(() -> renovacionPremiumService.preparar(NEGOCIO_ID, AHORA))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(suscripcionRepository, pagoPremiumRepository);
    }

    @Test
    @DisplayName("No cobra otra vez a un negocio con Premium sin vencimiento")
    void prepararShouldRejectIndefinitePremium() {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));

        assertThatThrownBy(() -> renovacionPremiumService.preparar(NEGOCIO_ID, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un Premium vencido puede volver a contratar sin conservar fechas anteriores")
    void prepararShouldAllowExpiredPremium() {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = renovacionPremiumService.preparar(NEGOCIO_ID, AHORA);

        assertThat(resultado.renovacion().getPrimerCobroEn()).isEqualTo(AHORA);
        assertThat(resultado.nueva()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 999_999, 1_000_000, 353_444_000, 999_999_999})
    @DisplayName("El primer cobro usa segundos completos sin anticipar el fin de la prueba")
    void prepararShouldNeverRoundBeforeTrialEnd(int nanos) {
        givenCatalogo(premium());
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setFinEn(AHORA.plusDays(4).plusNanos(nanos));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = renovacionPremiumService.preparar(NEGOCIO_ID, AHORA);

        assertThat(resultado.renovacion().getPrimerCobroEn()).isEqualTo(AHORA.plusDays(4).plusSeconds(1));
        assertThat(resultado.renovacion().getPrimerCobroEn()).isAfter(suscripcion.getFinEn());
    }

    @Test
    @DisplayName("Un contrato cancelado permite una contratación nueva conservando su historial")
    void prepararShouldAllowNewContractAfterCancellation() {
        RenovacionPremium anterior = renovacion();
        anterior.setEstado("CANCELADA");
        when(renovacionPremiumRepository.findUltimaByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(anterior));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion(GRATUITO_ID)));
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = renovacionPremiumService.preparar(NEGOCIO_ID, AHORA);

        assertThat(resultado.renovacion().getId()).isNotEqualTo(anterior.getId());
    }

    @Test
    @DisplayName("Un contrato inexistente no se puede consultar")
    void obtenerShouldRejectUnknownContract() {
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renovacionPremiumService.obtener(RENOVACION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un identificador inexistente no bloquea otro negocio")
    void actualizarContratoShouldRejectMissingBusinessProjection() {
        when(renovacionPremiumRepository.findNegocioIdById(RENOVACION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renovacionPremiumService.actualizarContrato(RENOVACION_ID,
                contrato(renovacion(), "pending", AHORA), AHORA)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(usoPlanRepository);
    }

    @Test
    @DisplayName("Sin suscripción no se inicia un cobro")
    void prepararShouldRejectMissingSubscription() {
        givenCatalogo(premium());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renovacionPremiumService.preparar(NEGOCIO_ID, AHORA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sin catálogo no se inventa un precio")
    void prepararShouldRejectMissingCatalog() {
        when(renovacionPremiumRepository.findUltimaByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renovacionPremiumService.preparar(NEGOCIO_ID, AHORA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No se puede cancelar una renovación inexistente del negocio")
    void solicitarCancelacionShouldRejectMissingContract() {
        when(renovacionPremiumRepository.findUltimaByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renovacionPremiumService.solicitarCancelacion(NEGOCIO_ID, AHORA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    @DisplayName("No acepta cambios remotos de referencia, precio, moneda, frecuencia, fecha ni identidad")
    void actualizarContratoShouldRejectMismatchedContract(int caso) {
        RenovacionPremium renovacion = givenRenovacion();
        ContratoRemoto base = contrato(renovacion, "pending", AHORA);
        renovacion.setProveedorId(base.id());
        ContratoRemoto remoto = new ContratoRemoto(caso == 6 ? "otro" : base.id(),
                caso == 0 ? UUID.randomUUID() : base.referencia(), base.enlace(),
                caso == 7 ? "desconocido" : base.estado(), caso == 1 ? BigDecimal.ONE : base.importe(),
                caso == 2 ? "USD" : base.moneda(), caso == 3 ? 1 : 30, caso == 4 ? "months" : "days",
                caso == 5 ? AHORA.minusSeconds(1) : AHORA, base.proximoCobroEn(), base.modificadoEn());

        assertThatThrownBy(() -> renovacionPremiumService.actualizarContrato(RENOVACION_ID, remoto, AHORA))
                .isInstanceOf(PasarelaPagoException.class);

        verify(renovacionPremiumRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "authorized", "paused", "cancelled", "canceled"})
    @DisplayName("Mapea los estados del proveedor sin deducir un pago aprobado")
    void actualizarContratoShouldMapRemoteStatuses(String estado) {
        RenovacionPremium renovacion = givenRenovacion();
        renovacion.setProveedorId(null);
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        RenovacionPremium resultado = renovacionPremiumService.actualizarContrato(RENOVACION_ID,
                contrato(renovacion, estado, AHORA), AHORA);

        assertThat(resultado.getSincronizadoEn()).isEqualTo(AHORA);
        assertThat(resultado.getProveedorId()).startsWith("mp-");
        verifyNoInteractions(suscripcionRepository, pagoPremiumRepository);
    }

    @Test
    @DisplayName("Una consulta remota anterior no sobrescribe la versión más nueva")
    void actualizarContratoShouldIgnoreOlderSnapshot() {
        RenovacionPremium renovacion = givenRenovacion();
        renovacion.setProveedorId("mp-" + RENOVACION_ID);
        renovacion.setModificadoProveedorEn(AHORA.plusSeconds(1));
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        renovacionPremiumService.actualizarContrato(RENOVACION_ID, contrato(renovacion, "pending", AHORA), AHORA);

        assertThat(renovacion.getEstado()).isEqualTo("AUTORIZADA");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("Un pago con referencia, contrato, importe, moneda o fecha ajenos no habilita acceso")
    void aplicarPagoShouldRejectMismatchedInvoice(int caso) {
        RenovacionPremium renovacion = givenRenovacion();
        PagoRemoto base = pago(renovacion, "factura", "approved", AHORA, AHORA);
        PagoRemoto remoto = new PagoRemoto(base.facturaId(), base.pagoId(), caso == 1 ? "otro" : base.contratoId(),
                caso == 0 ? UUID.randomUUID() : base.referencia(), caso == 2 ? BigDecimal.ONE : base.importe(),
                caso == 3 ? "USD" : base.moneda(), base.estado(), caso == 4 ? AHORA.minusSeconds(1) : AHORA, AHORA);

        assertThatThrownBy(() -> renovacionPremiumService.aplicarPago(RENOVACION_ID, remoto, AHORA))
                .isInstanceOf(PasarelaPagoException.class);

        verifyNoInteractions(pagoPremiumRepository, suscripcionRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Una factura existente no puede cambiar de negocio ni de período")
    void aplicarPagoShouldRejectInvoiceReassignment(boolean otroContrato) {
        RenovacionPremium renovacion = givenRenovacion();
        when(pagoPremiumRepository.findByFacturaId("factura")).thenReturn(Optional.of(PagoPremium.builder()
                .renovacionId(otroContrato ? UUID.randomUUID() : RENOVACION_ID)
                .inicioEn(otroContrato ? AHORA : AHORA.minusDays(1)).build()));

        assertThatThrownBy(() -> renovacionPremiumService.aplicarPago(RENOVACION_ID,
                pago(renovacion, "factura", "approved", AHORA, AHORA), AHORA))
                .isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Una suscripción Premium heredada no se degrada sin historial de pagos de esta integración")
    void recalcularShouldPreserveLegacyPremium() {
        givenRecalculo(suscripcion(PREMIUM_ID));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        when(pagoPremiumRepository.existeByNegocioId(NEGOCIO_ID)).thenReturn(false);

        renovacionPremiumService.recalcular(NEGOCIO_ID, AHORA);

        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una prueba sin pagos conserva su vigencia al conciliar")
    void recalcularShouldPreserveTrial() {
        Suscripcion suscripcion = suscripcion(PREMIUM_ID);
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        givenRecalculo(suscripcion);

        renovacionPremiumService.recalcular(NEGOCIO_ID, AHORA);

        verifyNoInteractions(planRepository);
    }

    private void givenCatalogo(Plan plan) {
        when(renovacionPremiumRepository.findUltimaByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(plan));
    }

    private RenovacionPremium givenRenovacion() {
        RenovacionPremium renovacion = renovacion();
        when(renovacionPremiumRepository.findNegocioIdById(RENOVACION_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.of(renovacion));
        return renovacion;
    }

    private void givenRecalculo(Suscripcion suscripcion) {
        when(pagoPremiumRepository.findVigente(NEGOCIO_ID, AHORA)).thenReturn(Optional.empty());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
    }

    private Plan premium() {
        return Plan.builder().id(PREMIUM_ID).codigo(CodigoPlan.PREMIUM).configurado(true)
                .limitesConfigurados(true).precioMensual(IMPORTE).moneda("ARS").build();
    }

    private Suscripcion suscripcion(UUID planId) {
        return Suscripcion.builder().negocioId(NEGOCIO_ID).planId(planId).estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(AHORA.minusDays(1)).build();
    }
}
