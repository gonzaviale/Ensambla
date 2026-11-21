package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.support.CobroCajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import com.ensambla.backend.infrastructure.repository.entity.VentaCajaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVentaCajaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.VentaCajaRepositoryMapper;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VentaCajaRepositoryImplTest {

    private final IVentaCajaJpaRepository jpa = mock(IVentaCajaJpaRepository.class);
    private final VentaCajaRepositoryMapper mapper = new VentaCajaRepositoryMapper();
    private final VentaCajaRepositoryImpl repositorio = new VentaCajaRepositoryImpl(jpa, mapper);
    private final VentaCaja venta = new VentaCaja(CajaTestData.OPERACION, CajaTestData.NEGOCIO,
            CajaTestData.PROYECTO, "huella", List.of(), MedioPagoCaja.TRANSFERENCIA, BigDecimal.TEN,
            BigDecimal.TEN, BigDecimal.ZERO, CajaTestData.AHORA);

    private List<VentaCaja> ventas;
    private VentaCaja guardada;
    private Optional<VentaCaja> encontrada;
    private VentaCaja ventaActual;
    private VentaCajaEntity entidadActual;
    private Throwable rechazo;

    @Test
    @DisplayName("El resumen del proyecto incluye sus ventas de cualquier inventario y cierre")
    void elResumenDelProyectoIncluyeSusVentasDeCualquierInventarioYCierre() {
        when(jpa.findByNegocioIdAndProyectoIdOrderByCreadoEnAscIdAsc(venta.negocioId().valor(),
                venta.proyectoId().valor())).thenReturn(List.of(mapper.toEntity(venta)));

        cuandoSeListanLasVentasDelProyecto();

        entoncesSeListaLaVenta();
    }

    @Test
    @DisplayName("Guarda todos los datos históricos y los ids UUID en infraestructura")
    void guardaTodosLosDatosHistoricosYLosIdsUuidEnInfraestructura() {
        dadoQueGuardarDevuelveLaEntidadRecibida();

        cuandoSeGuardaLaVenta();

        entoncesSeGuardaLaEntidadConTodosLosDatosHistoricos();
    }

    @Test
    @DisplayName("Busca la operación sólo dentro del negocio de la sesión")
    void buscaLaOperacionSoloDentroDelNegocioDeLaSesion() {
        when(jpa.findByIdAndNegocioId(venta.id().valor(), venta.negocioId().valor()))
                .thenReturn(Optional.of(mapper.toEntity(venta)));

        cuandoSeBuscaLaOperacionPorId();

        entoncesSeEncuentraLaVenta();
    }

    @Test
    @DisplayName("Una operación ajena o ausente no expone información")
    void unaOperacionAjenaOAusenteNoExponeInformacion() {
        when(jpa.findByIdAndNegocioId(venta.id().valor(), venta.negocioId().valor())).thenReturn(Optional.empty());

        cuandoSeBuscaLaOperacionPorId();

        entoncesNoSeEncuentraNingunaOperacion();
    }

    @Test
    @DisplayName("Actualizar cobros conserva el cierre y los identificadores originales de la venta")
    void actualizarCobrosConservaElCierreYLosIdentificadoresOriginalesDeLaVenta() {
        dadoUnaVentaCerradaQueYaExiste();

        cuandoSeActualizanLosCobrosAlDiaSiguiente();

        entoncesSeConservanElCierreYLosDatosYSeActualizaLaFecha();
    }

    @Test
    @DisplayName("No actualiza los pagos de una venta ajena o inexistente")
    void noActualizaLosPagosDeUnaVentaAjenaOInexistente() {
        cuandoSeIntentaActualizarLosCobrosDeUnaVentaInexistente();

        entoncesSeRechazaPorVentaInexistente();
    }

    @Test
    @DisplayName("La cuenta de clientes lista sólo las ventas del proyecto e inventario elegidos")
    void laCuentaDeClientesListaSoloLasVentasDelProyectoEInventarioElegidos() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnAscIdAsc(CajaTestData.NEGOCIO.valor(),
                CajaTestData.PROYECTO.valor(), CajaTestData.INVENTARIO.valor()))
                .thenReturn(List.of(mapper.toEntity(venta)));

        cuandoSeListanTodasLasVentasDelInventario();

        entoncesSeListaLaVenta();
    }

    @Test
    @DisplayName("La carga en lote de deudas conserva los ids y el filtro por negocio")
    void laCargaEnLoteDeDeudasConservaLosIdsYElFiltroPorNegocio() {
        when(jpa.findByNegocioIdAndIdIn(CajaTestData.NEGOCIO.valor(), List.of(venta.id().valor())))
                .thenReturn(List.of(mapper.toEntity(venta)));

        cuandoSeCarganLasVentasPorIds();

        entoncesSeListaLaVenta();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveLaEntidadRecibida() {
        when(jpa.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnaVentaCerradaQueYaExiste() {
        ventaActual = CobroCajaTestData.venta();
        entidadActual = mapper.toEntity(ventaActual);
        entidadActual.setCierreId(CierreCajaTestData.CIERRE.valor());
        when(jpa.findByIdAndNegocioId(ventaActual.id().valor(), ventaActual.negocioId().valor()))
                .thenReturn(Optional.of(entidadActual));
    }

    // --- cuando ---
    private void cuandoSeListanLasVentasDelProyecto() {
        ventas = repositorio.listarPorProyecto(venta.negocioId(), venta.proyectoId());
    }

    private void cuandoSeGuardaLaVenta() {
        guardada = repositorio.save(venta);
    }

    private void cuandoSeBuscaLaOperacionPorId() {
        encontrada = repositorio.findByIdAndNegocioId(venta.id(), venta.negocioId());
    }

    private void cuandoSeActualizanLosCobrosAlDiaSiguiente() {
        repositorio.actualizarCobros(ventaActual, CajaTestData.AHORA.plusDays(1));
    }

    private void cuandoSeIntentaActualizarLosCobrosDeUnaVentaInexistente() {
        rechazo = catchThrowable(() -> repositorio.actualizarCobros(venta, CajaTestData.AHORA));
    }

    private void cuandoSeListanTodasLasVentasDelInventario() {
        ventas = repositorio.listarTodas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO);
    }

    private void cuandoSeCarganLasVentasPorIds() {
        ventas = repositorio.listarPorIds(CajaTestData.NEGOCIO, List.of(venta.id()));
    }

    // --- entonces ---
    private void entoncesSeListaLaVenta() {
        assertThat(ventas).containsExactly(venta);
    }

    private void entoncesSeGuardaLaEntidadConTodosLosDatosHistoricos() {
        ArgumentCaptor<VentaCajaEntity> captor = ArgumentCaptor.forClass(VentaCajaEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(guardada).isEqualTo(venta);
        assertThat(captor.getValue().getId()).isEqualTo(venta.id().valor());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(venta.negocioId().valor());
        assertThat(captor.getValue().getProyectoId()).isEqualTo(venta.proyectoId().valor());
        assertThat(captor.getValue().getDatos()).isEqualTo(venta);
        assertThat(captor.getValue().getCreadoEn()).isEqualTo(venta.creadoEn());
        assertThat(captor.getValue().getActualizadoEn()).isEqualTo(venta.creadoEn());
    }

    private void entoncesSeEncuentraLaVenta() {
        assertThat(encontrada).contains(venta);
    }

    private void entoncesNoSeEncuentraNingunaOperacion() {
        assertThat(encontrada).isEmpty();
    }

    private void entoncesSeConservanElCierreYLosDatosYSeActualizaLaFecha() {
        verify(jpa).save(entidadActual);
        assertThat(entidadActual.getDatos()).isEqualTo(ventaActual);
        assertThat(entidadActual.getCierreId()).isEqualTo(CierreCajaTestData.CIERRE.valor());
        assertThat(entidadActual.getActualizadoEn()).isEqualTo(CajaTestData.AHORA.plusDays(1));
    }

    private void entoncesSeRechazaPorVentaInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
