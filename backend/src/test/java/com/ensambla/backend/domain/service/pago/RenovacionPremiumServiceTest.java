package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.RenovacionesDePrueba;
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

import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.contrato;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

    private RenovacionPremium renovacion;

    private RenovacionPremium resultado;

    private Throwable rechazo;

    @Test
    @DisplayName("Un contrato inexistente no se puede consultar")
    void unContratoInexistenteNoSePuedeConsultar() {
        dadoUnContratoInexistente();

        cuandoSeConsultaElContrato();

        entoncesSeRechazaPorRecursoInexistente();
    }

    @Test
    @DisplayName("Un identificador inexistente no bloquea a otro negocio")
    void unIdentificadorInexistenteNoBloqueaAOtroNegocio() {
        dadoUnIdentificadorSinNegocio();

        cuandoElProveedorInformaUnContratoPendiente();

        entoncesSeRechazaPorRecursoInexistenteSinBloquearNingunNegocio();
    }

    @Test
    @DisplayName("El proveedor no puede cambiar la referencia de la renovación")
    void elProveedorNoPuedeCambiarLaReferenciaDeLaRenovacion() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoDeOtraRenovacion();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede cambiar el precio de la renovación")
    void elProveedorNoPuedeCambiarElPrecioDeLaRenovacion() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoConOtroPrecio();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede cambiar la moneda de la renovación")
    void elProveedorNoPuedeCambiarLaMonedaDeLaRenovacion() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoConOtraMoneda();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede cambiar la frecuencia de cobro")
    void elProveedorNoPuedeCambiarLaFrecuenciaDeCobro() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoConOtraFrecuencia();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede cambiar la unidad de tiempo del cobro")
    void elProveedorNoPuedeCambiarLaUnidadDeTiempoDelCobro() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoConOtraUnidadDeTiempo();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede cambiar la fecha de inicio")
    void elProveedorNoPuedeCambiarLaFechaDeInicio() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoConOtraFechaDeInicio();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede reemplazar su contrato por otro")
    void elProveedorNoPuedeReemplazarSuContratoPorOtro() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoDeOtroContratoDelProveedor();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @Test
    @DisplayName("El proveedor no puede informar un estado desconocido")
    void elProveedorNoPuedeInformarUnEstadoDesconocido() {
        dadoUnaRenovacionConContratoDelProveedor();

        cuandoElProveedorInformaUnContratoConEstadoDesconocido();

        entoncesSeRechazaElContratoSinGuardarNada();
    }

    @ParameterizedTest(name = "con el estado {0} del proveedor")
    @ValueSource(strings = {"pending", "authorized", "paused", "cancelled", "canceled"})
    @DisplayName("Un estado del proveedor se sincroniza sin dar un pago por aprobado")
    void unEstadoDelProveedorSeSincronizaSinDarUnPagoPorAprobado(String estado) {
        dadoUnaRenovacionSinContratoEnElProveedor();

        cuandoElProveedorInformaUnContratoEnEstado(estado);

        entoncesLaRenovacionQuedaSincronizadaConElContratoDelProveedor();
    }

    @Test
    @DisplayName("Una consulta anterior no pisa la versión más nueva")
    void unaConsultaAnteriorNoPisaLaVersionMasNueva() {
        dadoUnaRenovacionModificadaPorElProveedorUnSegundoDespues();

        cuandoLlegaUnaConsultaAnteriorDelProveedor();

        entoncesLaRenovacionSigueAutorizada();
    }

    private void dadoUnContratoInexistente() {
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnIdentificadorSinNegocio() {
        when(renovacionPremiumRepository.findNegocioIdById(RENOVACION_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaRenovacionConContratoDelProveedor() {
        dadoUnaRenovacionGuardada();
        renovacion.setProveedorId(contratoPendiente().id());
    }

    private void dadoUnaRenovacionSinContratoEnElProveedor() {
        dadoUnaRenovacionGuardada();
        renovacion.setProveedorId(null);
        dadoQueGuardarDevuelveLoGuardado();
    }

    private void dadoUnaRenovacionModificadaPorElProveedorUnSegundoDespues() {
        dadoUnaRenovacionGuardada();
        renovacion.setProveedorId("mp-" + RENOVACION_ID);
        renovacion.setModificadoProveedorEn(AHORA.plusSeconds(1));
        dadoQueGuardarDevuelveLoGuardado();
    }

    private void dadoUnaRenovacionGuardada() {
        renovacion = renovacion();
        when(renovacionPremiumRepository.findNegocioIdById(RENOVACION_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.of(renovacion));
    }

    private void dadoQueGuardarDevuelveLoGuardado() {
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void cuandoSeConsultaElContrato() {
        rechazo = catchThrowable(() -> renovacionPremiumService.obtener(RENOVACION_ID));
    }

    private void cuandoElProveedorInformaUnContratoPendiente() {
        rechazo = catchThrowable(() -> actualizarCon(contrato(renovacion(), "pending", AHORA)));
    }

    private void cuandoElProveedorInformaUnContratoDeOtraRenovacion() {
        intentarActualizarCon(RenovacionesDePrueba.contratoDeOtraRenovacion(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoConOtroPrecio() {
        intentarActualizarCon(RenovacionesDePrueba.contratoConOtroPrecio(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoConOtraMoneda() {
        intentarActualizarCon(RenovacionesDePrueba.contratoConOtraMoneda(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoConOtraFrecuencia() {
        intentarActualizarCon(RenovacionesDePrueba.contratoConOtraFrecuencia(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoConOtraUnidadDeTiempo() {
        intentarActualizarCon(RenovacionesDePrueba.contratoConOtraUnidadDeTiempo(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoConOtraFechaDeInicio() {
        intentarActualizarCon(RenovacionesDePrueba.contratoConOtraFechaDeInicio(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoDeOtroContratoDelProveedor() {
        intentarActualizarCon(RenovacionesDePrueba.contratoDeOtroContratoDelProveedor(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoConEstadoDesconocido() {
        intentarActualizarCon(RenovacionesDePrueba.contratoConEstadoDesconocido(contratoPendiente()));
    }

    private void cuandoElProveedorInformaUnContratoEnEstado(String estado) {
        resultado = actualizarCon(contrato(renovacion, estado, AHORA));
    }

    private void cuandoLlegaUnaConsultaAnteriorDelProveedor() {
        actualizarCon(contratoPendiente());
    }

    private void entoncesSeRechazaPorRecursoInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeRechazaPorRecursoInexistenteSinBloquearNingunNegocio() {
        entoncesSeRechazaPorRecursoInexistente();
        verifyNoInteractions(usoPlanRepository);
    }

    private void entoncesSeRechazaElContratoSinGuardarNada() {
        assertThat(rechazo).isInstanceOf(PasarelaPagoException.class);
        verify(renovacionPremiumRepository, never()).save(any());
    }

    private void entoncesLaRenovacionQuedaSincronizadaConElContratoDelProveedor() {
        assertThat(resultado.getSincronizadoEn()).isEqualTo(AHORA);
        assertThat(resultado.getProveedorId()).startsWith("mp-");
    }

    private void entoncesLaRenovacionSigueAutorizada() {
        assertThat(renovacion.getEstado()).isEqualTo("AUTORIZADA");
    }

    private ContratoRemoto contratoPendiente() {
        return contrato(renovacion, "pending", AHORA);
    }

    private RenovacionPremium actualizarCon(ContratoRemoto remoto) {
        return renovacionPremiumService.actualizarContrato(RENOVACION_ID, remoto, AHORA);
    }

    private void intentarActualizarCon(ContratoRemoto remoto) {
        rechazo = catchThrowable(() -> actualizarCon(remoto));
    }
}
