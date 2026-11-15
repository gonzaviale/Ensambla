package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.infrastructure.repository.entity.CobroCajaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICobroCajaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CobroCajaRepositoryMapper;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CobroCajaRepositoryImplTest {

    @Mock
    private ICobroCajaJpaRepository jpa;
    private final CobroCajaRepositoryMapper mapper = new CobroCajaRepositoryMapper();
    private CobroCajaRepositoryImpl repositorio;
    private final CobroCaja cobro = CobroCajaTestData.cobro();

    @BeforeEach
    void setUp() {
        repositorio = new CobroCajaRepositoryImpl(jpa, mapper);
    }

    @Test
    @DisplayName("Guarda el cobro inmutable con ids, fecha y todas sus aplicaciones")
    void saveShouldMapCollection() {
        when(jpa.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        CobroCaja guardado = repositorio.save(cobro);

        ArgumentCaptor<CobroCajaEntity> captor = ArgumentCaptor.forClass(CobroCajaEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(guardado).isEqualTo(cobro);
        assertThat(captor.getValue().getId()).isEqualTo(cobro.id().valor());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(cobro.negocioId().valor());
        assertThat(captor.getValue().getProyectoId()).isEqualTo(cobro.proyectoId().valor());
        assertThat(captor.getValue().getInventarioId()).isEqualTo(cobro.inventarioId().valor());
        assertThat(captor.getValue().getDatos()).isEqualTo(cobro);
        assertThat(captor.getValue().getCreadoEn()).isEqualTo(cobro.creadoEn());
        assertThat(captor.getValue().getActualizadoEn()).isEqualTo(cobro.creadoEn());
    }

    @Test
    @DisplayName("Busca cobros sólo dentro del negocio autenticado")
    void findShouldScopeByBusiness() {
        when(jpa.findByIdAndNegocioId(cobro.id().valor(), cobro.negocioId().valor())).thenReturn(Optional.of(mapper.toEntity(cobro)));

        var encontrado = repositorio.findByIdAndNegocioId(cobro.id(), cobro.negocioId());

        assertThat(encontrado).contains(cobro);
    }

    @Test
    @DisplayName("Lista todos los pagos del inventario en su orden persistido")
    void listarTodosShouldScopeByInventory() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnAscIdAsc(cobro.negocioId().valor(),
                cobro.proyectoId().valor(), cobro.inventarioId().valor())).thenReturn(List.of(mapper.toEntity(cobro)));

        var encontrados = repositorio.listarTodos(cobro.negocioId(), cobro.proyectoId(), cobro.inventarioId());

        assertThat(encontrados).containsExactly(cobro);
    }

    @Test
    @DisplayName("Lista sólo los cobros que todavía no tienen cierre")
    void listarAbiertosShouldScopeUnclosedCollections() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdAndCierreIdIsNullOrderByCreadoEnAscIdAsc(cobro.negocioId().valor(),
                cobro.proyectoId().valor(), cobro.inventarioId().valor())).thenReturn(List.of(mapper.toEntity(cobro)));

        var encontrados = repositorio.listarAbiertos(cobro.negocioId(), cobro.proyectoId(), cobro.inventarioId());

        assertThat(encontrados).containsExactly(cobro);
    }

    @Test
    @DisplayName("El rango de fechas usa un inicio incluido y un fin excluido")
    void listarEntreShouldUseExactDateBounds() {
        var desde = CajaTestData.AHORA;
        var hasta = desde.plusDays(1);
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnAscIdAsc(
                cobro.negocioId().valor(), cobro.proyectoId().valor(), cobro.inventarioId().valor(), desde, hasta))
                .thenReturn(List.of(mapper.toEntity(cobro)));

        var encontrados = repositorio.listarEntre(cobro.negocioId(), cobro.proyectoId(), cobro.inventarioId(), desde, hasta);

        assertThat(encontrados).containsExactly(cobro);
    }

    @Test
    @DisplayName("El cierre vincula los ids del cobro sin alterar sus datos")
    void marcarCerradosShouldPassBusinessAndIds() {
        repositorio.marcarCerrados(cobro.negocioId(), CierreCajaTestData.CIERRE, List.of(cobro.id()));

        verify(jpa).marcarCerrados(cobro.negocioId().valor(), CierreCajaTestData.CIERRE.valor(), List.of(cobro.id().valor()));
    }
}
