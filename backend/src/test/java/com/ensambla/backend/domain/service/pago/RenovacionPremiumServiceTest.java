package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.USUARIO_ID;
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

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @InjectMocks
    private RenovacionPremiumService renovacionPremiumService;

    @Test
    @DisplayName("Un contrato inexistente no se puede consultar")
    void obtenerShouldRejectUnknownContract() {
        givenRenovacionInexistente();

        assertThatThrownBy(() -> renovacionPremiumService.obtener(RENOVACION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un identificador inexistente no bloquea otro negocio")
    void actualizarContratoShouldRejectMissingBusinessProjection() {
        givenRenovacionSinCuenta();

        assertThatThrownBy(() -> renovacionPremiumService.actualizarContrato(RENOVACION_ID,
                contrato(renovacion(), "pending", AHORA), AHORA)).isInstanceOf(ResourceNotFoundException.class);

        thenNoSeBloqueaNingunNegocio();
    }

    @ParameterizedTest
    @MethodSource("contratosQueNoCorresponden")
    @DisplayName("No acepta cambios remotos de referencia, precio, moneda, frecuencia, fecha ni identidad")
    void actualizarContratoShouldRejectMismatchedContract(UnaryOperator<ContratoRemoto> desvio) {
        RenovacionPremium renovacion = givenRenovacionConContratoDelProveedor();
        ContratoRemoto remoto = desvio.apply(contrato(renovacion, "pending", AHORA));

        assertThatThrownBy(() -> renovacionPremiumService.actualizarContrato(RENOVACION_ID, remoto, AHORA))
                .isInstanceOf(PasarelaPagoException.class);

        thenNoSeGuardaNingunaRenovacion();
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "authorized", "paused", "cancelled", "canceled"})
    @DisplayName("Mapea los estados del proveedor sin deducir un pago aprobado")
    void actualizarContratoShouldMapRemoteStatuses(String estado) {
        RenovacionPremium renovacion = givenRenovacion();
        renovacion.setProveedorId(null);
        givenQueGuardarDevuelveLoGuardado();

        RenovacionPremium resultado = renovacionPremiumService.actualizarContrato(RENOVACION_ID,
                contrato(renovacion, estado, AHORA), AHORA);

        assertThat(resultado.getSincronizadoEn()).isEqualTo(AHORA);
        assertThat(resultado.getProveedorId()).startsWith("mp-");
    }

    @Test
    @DisplayName("Una consulta remota anterior no sobrescribe la versión más nueva")
    void actualizarContratoShouldIgnoreOlderSnapshot() {
        RenovacionPremium renovacion = givenRenovacion();
        renovacion.setProveedorId("mp-" + RENOVACION_ID);
        renovacion.setModificadoProveedorEn(AHORA.plusSeconds(1));
        givenQueGuardarDevuelveLoGuardado();

        renovacionPremiumService.actualizarContrato(RENOVACION_ID, contrato(renovacion, "pending", AHORA), AHORA);

        assertThat(renovacion.getEstado()).isEqualTo("AUTORIZADA");
    }

    static Stream<Arguments> contratosQueNoCorresponden() {
        return Stream.of(
                desvio("de otra renovación", base -> contratoCon(base, RenovacionId.nuevo(), base.importe())),
                desvio("con otro precio", base -> contratoCon(base, base.referencia(), BigDecimal.ONE)),
                desvio("con otra moneda", base -> contratoConMoneda(base, "USD")),
                desvio("con otra frecuencia", base -> contratoConPeriodo(base, 1, base.unidad())),
                desvio("con otra unidad de tiempo", base -> contratoConPeriodo(base, base.frecuencia(), "months")),
                desvio("con otra fecha de inicio", base -> contratoConInicio(base, AHORA.minusSeconds(1))),
                desvio("de otro contrato del proveedor", base -> contratoConProveedor(base, "otro")),
                desvio("con un estado desconocido", base -> contratoConEstado(base, "desconocido")));
    }

    private static Arguments desvio(String descripcion, UnaryOperator<ContratoRemoto> desvio) {
        return Arguments.of(Named.of(descripcion, desvio));
    }

    private static ContratoRemoto contratoCon(ContratoRemoto base, RenovacionId referencia, BigDecimal importe) {
        return new ContratoRemoto(base.id(), referencia, base.enlace(), base.estado(), importe, base.moneda(),
                base.frecuencia(), base.unidad(), base.inicioEn(), base.proximoCobroEn(), base.modificadoEn());
    }

    private static ContratoRemoto contratoConMoneda(ContratoRemoto base, String moneda) {
        return new ContratoRemoto(base.id(), base.referencia(), base.enlace(), base.estado(), base.importe(), moneda,
                base.frecuencia(), base.unidad(), base.inicioEn(), base.proximoCobroEn(), base.modificadoEn());
    }

    private static ContratoRemoto contratoConPeriodo(ContratoRemoto base, int frecuencia, String unidad) {
        return new ContratoRemoto(base.id(), base.referencia(), base.enlace(), base.estado(), base.importe(),
                base.moneda(), frecuencia, unidad, base.inicioEn(), base.proximoCobroEn(), base.modificadoEn());
    }

    private static ContratoRemoto contratoConInicio(ContratoRemoto base, OffsetDateTime inicio) {
        return new ContratoRemoto(base.id(), base.referencia(), base.enlace(), base.estado(), base.importe(),
                base.moneda(), base.frecuencia(), base.unidad(), inicio, base.proximoCobroEn(), base.modificadoEn());
    }

    private static ContratoRemoto contratoConProveedor(ContratoRemoto base, String proveedorId) {
        return new ContratoRemoto(proveedorId, base.referencia(), base.enlace(), base.estado(), base.importe(),
                base.moneda(), base.frecuencia(), base.unidad(), base.inicioEn(), base.proximoCobroEn(),
                base.modificadoEn());
    }

    private static ContratoRemoto contratoConEstado(ContratoRemoto base, String estado) {
        return new ContratoRemoto(base.id(), base.referencia(), base.enlace(), estado, base.importe(), base.moneda(),
                base.frecuencia(), base.unidad(), base.inicioEn(), base.proximoCobroEn(), base.modificadoEn());
    }

    private void givenRenovacionInexistente() {
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.empty());
    }

    private void givenRenovacionSinCuenta() {
        when(renovacionPremiumRepository.findUsuarioIdById(RENOVACION_ID)).thenReturn(Optional.empty());
    }

    private void givenQueGuardarDevuelveLoGuardado() {
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void thenNoSeBloqueaNingunNegocio() {
        verifyNoInteractions(usoPlanRepository);
    }

    private void thenNoSeGuardaNingunaRenovacion() {
        verify(renovacionPremiumRepository, never()).save(any());
    }

    private RenovacionPremium givenRenovacionConContratoDelProveedor() {
        RenovacionPremium renovacion = givenRenovacion();
        renovacion.setProveedorId(contrato(renovacion, "pending", AHORA).id());
        return renovacion;
    }

    private RenovacionPremium givenRenovacion() {
        RenovacionPremium renovacion = renovacion();
        when(renovacionPremiumRepository.findUsuarioIdById(RENOVACION_ID)).thenReturn(Optional.of(USUARIO_ID));
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.of(renovacion));
        return renovacion;
    }
}
