package com.ensambla.backend.domain.useCase.caja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaResponse;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierreRequest;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionesCajaTest {

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final SistemaRepository sistemas = mock(SistemaRepository.class);
    private final VentaCajaRepository ventas = mock(VentaCajaRepository.class);
    private final CierreCajaRepository cierres = mock(CierreCajaRepository.class);
    private final UsoPlanRepository uso = mock(UsoPlanRepository.class);
    private final TransaccionPort transaccion = new TransaccionDirecta();
    private final Clock clock = Clock.fixed(CajaTestData.AHORA.toInstant(), ZoneOffset.UTC);
    private final CerrarCaja cerrar = new CerrarCaja(negocios, sistemas, ventas, cierres, uso, transaccion, clock);
    private final ConsultarCaja consultar = new ConsultarCaja(negocios, sistemas, ventas, cierres, uso, transaccion, clock);
    private final ObtenerCierre obtener = new ObtenerCierre(negocios, cierres);

    @Test
    @DisplayName("Cierra y vincula exactamente las ventas revisadas bajo el mismo bloqueo que los cobros")
    void cerrarShouldSaveAndAssignPeriod() {
        givenUsuario();
        when(ventas.listarAbiertas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CierreCajaTestData.venta()));

        CierreCajaItem respuesta = cerrar.execute(CierreCajaTestData.request());

        thenSnapshotIsComplete(respuesta);
        verify(uso).bloquearNegocio(CajaTestData.NEGOCIO);
        verify(sistemas).obtenerInventarioCaja(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO);
        verify(cierres).save(CierreCajaTestData.cierre());
        verify(ventas).marcarCerradas(CajaTestData.NEGOCIO, CierreCajaTestData.CIERRE, List.of(CajaTestData.OPERACION));
    }

    @Test
    @DisplayName("Reintentar el mismo cierre devuelve la copia original sin consumir ventas posteriores")
    void cerrarShouldReturnSameSnapshotWhenRetried() {
        givenUsuario();
        when(cierres.findByIdAndNegocioId(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(CierreCajaTestData.cierre()));

        CierreCajaItem respuesta = cerrar.execute(CierreCajaTestData.request());

        thenSnapshotIsComplete(respuesta);
        verify(ventas, never()).listarAbiertas(any(), any(), any());
        verify(cierres, never()).save(any());
    }

    @Test
    @DisplayName("El id de cierre no puede reutilizarse con datos distintos")
    void cerrarShouldRejectChangedRetry() {
        givenUsuario();
        CierreCaja anterior = CierreCajaTestData.cierre();
        when(cierres.findByIdAndNegocioId(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(anterior));
        CerrarCajaRequest base = CierreCajaTestData.request();
        CerrarCajaRequest cambiado = new CerrarCajaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.revision(), base.fondoInicial(), base.entradas(), base.retiros(), null, "");

        assertThatThrownBy(() -> cerrar.execute(cambiado)).isInstanceOf(DuplicateResourceException.class);

        verify(cierres, never()).save(any());
    }

    @Test
    @DisplayName("El día argentino se consulta aparte del período abierto")
    void consultarShouldSeparateCalendarDayAndOpenPeriod() {
        givenUsuario();
        LocalDate fecha = ZonaNegocio.dia(CajaTestData.AHORA);
        when(ventas.listarAbiertas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CierreCajaTestData.venta()));
        when(cierres.listar(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, 0))
                .thenReturn(List.of(CierreCajaTestData.cierre()));

        ConsultarCajaResponse respuesta = consultar.execute(new ConsultarCajaRequest(CajaTestData.USUARIO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, null, 0));

        assertThat(respuesta.fecha()).isEqualTo(fecha);
        assertThat(respuesta.dia().ventas()).isEmpty();
        assertThat(respuesta.abierta().total()).isEqualByComparingTo("2000");
        assertThat(respuesta.cierres().getFirst().id()).isEqualTo(CierreCajaTestData.CIERRE);
        assertThat(respuesta.cierres().getFirst().desde()).isEqualTo(CajaTestData.AHORA);
        assertThat(respuesta.cierres().getFirst().cerradoEn()).isEqualTo(CajaTestData.AHORA);
        assertThat(respuesta.cierres().getFirst().total()).isEqualByComparingTo("2000");
        assertThat(respuesta.cierres().getFirst().cantidadVentas()).isEqualTo(1);
        assertThat(respuesta.cierres().getFirst().diferencia()).isEqualByComparingTo("0");
        assertThat(respuesta.pagina()).isZero();
        assertThat(respuesta.hayMas()).isFalse();
        verify(ventas).listarEntre(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                ZonaNegocio.inicioDelDia(fecha), ZonaNegocio.inicioDelDia(fecha.plusDays(1)));
    }

    @Test
    @DisplayName("Permite consultar otra fecha y paginar los cierres")
    void consultarShouldSupportSelectedDayAndPagination() {
        givenUsuario();
        LocalDate fecha = LocalDate.of(2026, 10, 1);
        when(cierres.listar(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, 1))
                .thenReturn(Collections.nCopies(20, CierreCajaTestData.cierre()));

        ConsultarCajaResponse respuesta = consultar.execute(new ConsultarCajaRequest(CajaTestData.USUARIO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, fecha, 1));

        assertThat(respuesta.fecha()).isEqualTo(fecha);
        assertThat(respuesta.pagina()).isEqualTo(1);
        assertThat(respuesta.hayMas()).isTrue();
    }

    @Test
    @DisplayName("El detalle de un cierre sólo se obtiene dentro del negocio autenticado")
    void obtenerShouldReturnOwnedSnapshot() {
        givenUsuario();
        when(cierres.findByIdAndNegocioId(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(CierreCajaTestData.cierre()));

        CierreCajaItem respuesta = obtener.execute(new ObtenerCierreRequest(CajaTestData.USUARIO, CierreCajaTestData.CIERRE));

        thenSnapshotIsComplete(respuesta);
    }

    @Test
    @DisplayName("Un cierre ajeno responde como inexistente")
    void obtenerShouldHideForeignClosure() {
        givenUsuario();

        assertThatThrownBy(() -> obtener.execute(new ObtenerCierreRequest(CajaTestData.USUARIO, CierreCajaId.nuevo())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenUsuario() {
        when(negocios.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    // --- assert ---
    private void thenSnapshotIsComplete(CierreCajaItem respuesta) {
        assertThat(respuesta.id()).isEqualTo(CierreCajaTestData.CIERRE);
        assertThat(respuesta.resumen().ventas()).hasSize(1);
        assertThat(respuesta.resumen().total()).isEqualByComparingTo("2000");
        assertThat(respuesta.resumen().efectivo()).isEqualByComparingTo("1000");
        assertThat(respuesta.resumen().transferencia()).isEqualByComparingTo("700");
        assertThat(respuesta.resumen().tarjeta()).isEqualByComparingTo("300");
        assertThat(respuesta.resumen().desde()).isEqualTo(CajaTestData.AHORA);
        assertThat(respuesta.resumen().revision()).isEqualTo(ResumenCaja.de(List.of(CierreCajaTestData.venta())).revision());
        assertThat(respuesta.fondoInicial()).isEqualByComparingTo("500");
        assertThat(respuesta.entradas()).isEqualByComparingTo("100");
        assertThat(respuesta.retiros()).isEqualByComparingTo("200");
        assertThat(respuesta.esperado()).isEqualByComparingTo("1400");
        assertThat(respuesta.contado()).isEqualByComparingTo("1400");
        assertThat(respuesta.diferencia()).isEqualByComparingTo("0");
        assertThat(respuesta.nota()).isEqualTo("Revisado");
        assertThat(respuesta.cerradoEn()).isEqualTo(CajaTestData.AHORA);
    }
}
