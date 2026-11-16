package com.ensambla.backend.domain.useCase.caja.registrarCobro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarCobroTest {

    @Mock
    private NegocioRepository negocios;
    @Mock
    private UsoPlanRepository uso;
    @Mock
    private VentaCajaRepository ventas;
    @Mock
    private CobroCajaRepository cobros;
    private final Clock clock = Clock.fixed(CajaTestData.AHORA.plusDays(1).toInstant(), ZoneOffset.UTC);
    private RegistrarCobro registrar;

    @BeforeEach
    void setUp() {
        registrar = new RegistrarCobro(negocios, uso, ventas, cobros, new TransaccionDirecta(), clock);
    }

    @Test
    @DisplayName("Un cobro reduce el saldo y se guarda con fecha propia bajo el bloqueo del negocio")
    void executeShouldPersistCollectionAndNewBalance() {
        givenVenta(CobroCajaTestData.venta());
        when(cobros.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = registrar.execute(CobroCajaTestData.request());

        ArgumentCaptor<VentaCaja> captor = ArgumentCaptor.forClass(VentaCaja.class);
        verify(ventas).actualizarCobros(captor.capture(), eq(CajaTestData.AHORA.plusDays(1)));
        assertThat(captor.getValue().saldo()).isEqualByComparingTo("600");
        assertThat(respuesta).isEqualTo(CobroCajaItem.de(CobroCajaTestData.cobro()));
        verify(uso).bloquearNegocio(CajaTestData.NEGOCIO);
    }

    @Test
    @DisplayName("Reintentar el mismo cobro devuelve el movimiento guardado sin reducir otra vez la deuda")
    void executeShouldReturnPreviousCollectionWhenRetried() {
        givenUsuario();
        when(cobros.findByIdAndNegocioId(CobroCajaTestData.COBRO, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(CobroCajaTestData.cobro()));

        var respuesta = registrar.execute(CobroCajaTestData.request());

        assertThat(respuesta.id()).isEqualTo(CobroCajaTestData.COBRO);
        verify(ventas, never()).actualizarCobros(any(), any());
    }

    @Test
    @DisplayName("Reutilizar el id de cobro con otro contenido responde conflicto")
    void executeShouldRejectChangedRetry() {
        givenUsuario();
        CobroCaja base = CobroCajaTestData.cobro();
        when(cobros.findByIdAndNegocioId(base.id(), base.negocioId())).thenReturn(Optional.of(new CobroCaja(
                base.id(), base.negocioId(), base.proyectoId(), base.inventarioId(), base.clienteId(), base.clienteNombre(),
                "otra huella", base.aplicaciones(), base.creadoEn())));

        assertThatThrownBy(() -> registrar.execute(CobroCajaTestData.request())).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Una venta ajena responde como inexistente")
    void executeShouldHideMissingSale() {
        givenUsuario();
        when(ventas.listarPorIds(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> registrar.execute(CobroCajaTestData.request())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No aplica cobros a ventas de otro proyecto")
    void executeShouldRejectWrongProject() {
        givenVenta(ventaCon(ProyectoId.nuevo(), CajaTestData.INVENTARIO, CajaTestData.CLIENTE));

        assertThatThrownBy(() -> registrar.execute(CobroCajaTestData.request())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No aplica cobros a ventas de otro inventario")
    void executeShouldRejectWrongInventory() {
        givenVenta(ventaCon(CajaTestData.PROYECTO, BloqueId.nuevo(), CajaTestData.CLIENTE));

        assertThatThrownBy(() -> registrar.execute(CobroCajaTestData.request())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No aplica una entrega de deuda a una venta anónima")
    void executeShouldRejectAnonymousSale() {
        givenVenta(ventaCon(CajaTestData.PROYECTO, CajaTestData.INVENTARIO, null));

        assertThatThrownBy(() -> registrar.execute(CobroCajaTestData.request())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una solicitud sin deudas seleccionadas")
    void executeShouldRejectEmptyApplications() {
        givenUsuario();

        assertThatThrownBy(() -> registrar.execute(new RegistrarCobroRequest(CajaTestData.USUARIO, CobroCajaTestData.COBRO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, List.of()))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("No permite mezclar deudas de clientes distintos en un mismo cobro")
    void executeShouldRejectDifferentCustomers() {
        givenUsuario();
        VentaCaja primera = CobroCajaTestData.venta();
        VentaCaja segunda = segundaVenta(RegistroId.nuevo());
        RegistrarCobroRequest request = solicitudDoble(segunda.id());
        when(ventas.listarPorIds(any(), any())).thenReturn(List.of(primera, segunda));

        assertThatThrownBy(() -> registrar.execute(request)).isInstanceOf(BusinessRuleException.class);

        verify(cobros, never()).save(any());
    }

    @Test
    @DisplayName("Un mismo pago puede reducir dos deudas del mismo cliente")
    void executeShouldApplyOneCollectionToMultipleSales() {
        givenUsuario();
        VentaCaja primera = CobroCajaTestData.venta();
        VentaCaja segunda = segundaVenta(CajaTestData.CLIENTE);
        when(ventas.listarPorIds(any(), any())).thenReturn(List.of(primera, segunda));
        when(cobros.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = registrar.execute(solicitudDoble(segunda.id()));

        assertThat(respuesta.importe()).isEqualByComparingTo("200");
        assertThat(respuesta.aplicaciones()).hasSize(2);
    }

    // --- arrange ---
    private void givenUsuario() {
        when(negocios.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    private void givenVenta(VentaCaja venta) {
        givenUsuario();
        when(ventas.listarPorIds(CajaTestData.NEGOCIO, List.of(CajaTestData.OPERACION))).thenReturn(List.of(venta));
    }

    // --- helpers ---
    private VentaCaja segundaVenta(RegistroId clienteId) {
        VentaCaja base = ventaCon(CajaTestData.PROYECTO, CajaTestData.INVENTARIO, clienteId);
        return new VentaCaja(VentaCajaId.nuevo(), base.negocioId(), base.proyectoId(), base.huella(), base.lineas(),
                base.medio(), base.total(), base.recibido(), base.vuelto(), base.creadoEn(), base.pagos(),
                base.inventarioId(), clienteId, base.clienteNombre());
    }

    private RegistrarCobroRequest solicitudDoble(VentaCajaId segundaId) {
        var pago = List.of(new RegistrarCobroRequest.PagoRequest("EFECTIVO", new BigDecimal("100")));
        return new RegistrarCobroRequest(CajaTestData.USUARIO, CobroCajaTestData.COBRO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(new RegistrarCobroRequest.AplicacionRequest(CajaTestData.OPERACION, pago),
                        new RegistrarCobroRequest.AplicacionRequest(segundaId, pago)));
    }

    private VentaCaja ventaCon(ProyectoId proyectoId, BloqueId inventarioId, RegistroId clienteId) {
        VentaCaja base = CobroCajaTestData.venta();
        return new VentaCaja(base.id(), base.negocioId(), proyectoId, base.huella(), base.lineas(), base.medio(), base.total(),
                base.recibido(), base.vuelto(), base.creadoEn(), base.pagos(), inventarioId, clienteId, base.clienteNombre());
    }
}
