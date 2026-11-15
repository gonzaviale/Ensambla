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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Guarda todos los datos históricos y los ids UUID en infraestructura")
    void saveShouldMapEntireSale() {
        when(jpa.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        VentaCaja guardada = repositorio.save(venta);

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

    @Test
    @DisplayName("Busca la operación sólo dentro del negocio de la sesión")
    void findShouldScopeByBusiness() {
        when(jpa.findByIdAndNegocioId(venta.id().valor(), venta.negocioId().valor()))
                .thenReturn(Optional.of(mapper.toEntity(venta)));

        Optional<VentaCaja> encontrada = repositorio.findByIdAndNegocioId(venta.id(), venta.negocioId());

        assertThat(encontrada).contains(venta);
    }

    @Test
    @DisplayName("Una operación ajena o ausente no expone información")
    void findShouldReturnEmptyWhenMissing() {
        when(jpa.findByIdAndNegocioId(venta.id().valor(), venta.negocioId().valor())).thenReturn(Optional.empty());

        Optional<VentaCaja> encontrada = repositorio.findByIdAndNegocioId(venta.id(), venta.negocioId());

        assertThat(encontrada).isEmpty();
    }

    @Test
    @DisplayName("Actualizar cobros conserva el cierre y los identificadores originales de la venta")
    void actualizarCobrosShouldKeepOriginalClosure() {
        VentaCaja actual = CobroCajaTestData.venta();
        VentaCajaEntity entity = mapper.toEntity(actual);
        entity.setCierreId(CierreCajaTestData.CIERRE.valor());
        when(jpa.findByIdAndNegocioId(actual.id().valor(), actual.negocioId().valor())).thenReturn(Optional.of(entity));

        repositorio.actualizarCobros(actual, CajaTestData.AHORA.plusDays(1));

        verify(jpa).save(entity);
        assertThat(entity.getDatos()).isEqualTo(actual);
        assertThat(entity.getCierreId()).isEqualTo(CierreCajaTestData.CIERRE.valor());
        assertThat(entity.getActualizadoEn()).isEqualTo(CajaTestData.AHORA.plusDays(1));
    }

    @Test
    @DisplayName("No actualiza los pagos de una venta ajena o inexistente")
    void actualizarCobrosShouldHideMissingSale() {
        assertThatThrownBy(() -> repositorio.actualizarCobros(venta, CajaTestData.AHORA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("La cuenta de clientes lista sólo las ventas del proyecto e inventario elegidos")
    void listarTodasShouldScopeByInventory() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnAscIdAsc(CajaTestData.NEGOCIO.valor(),
                CajaTestData.PROYECTO.valor(), CajaTestData.INVENTARIO.valor())).thenReturn(List.of(mapper.toEntity(venta)));

        List<VentaCaja> resultado = repositorio.listarTodas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO);

        assertThat(resultado).containsExactly(venta);
    }

    @Test
    @DisplayName("La carga en lote de deudas conserva los ids y el filtro por negocio")
    void listarPorIdsShouldScopeBatchByBusiness() {
        when(jpa.findByNegocioIdAndIdIn(CajaTestData.NEGOCIO.valor(), List.of(venta.id().valor())))
                .thenReturn(List.of(mapper.toEntity(venta)));

        List<VentaCaja> resultado = repositorio.listarPorIds(CajaTestData.NEGOCIO, List.of(venta.id()));

        assertThat(resultado).containsExactly(venta);
    }
}
