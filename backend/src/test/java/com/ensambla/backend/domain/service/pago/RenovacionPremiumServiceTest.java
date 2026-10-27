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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
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

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    @DisplayName("No acepta cambios remotos de referencia, precio, moneda, frecuencia, fecha ni identidad")
    void actualizarContratoShouldRejectMismatchedContract(int caso) {
        RenovacionPremium renovacion = givenRenovacion();
        ContratoRemoto base = contrato(renovacion, "pending", AHORA);
        renovacion.setProveedorId(base.id());
        ContratoRemoto remoto = new ContratoRemoto(caso == 6 ? "otro" : base.id(),
                caso == 0 ? RenovacionId.nuevo() : base.referencia(), base.enlace(),
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

    private RenovacionPremium givenRenovacion() {
        RenovacionPremium renovacion = renovacion();
        when(renovacionPremiumRepository.findNegocioIdById(RENOVACION_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.of(renovacion));
        return renovacion;
    }
}
