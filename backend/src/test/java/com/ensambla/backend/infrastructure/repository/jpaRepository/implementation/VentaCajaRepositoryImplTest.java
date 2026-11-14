package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
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
}
