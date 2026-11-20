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
    void dadoUnRepositorioDeCobros() {
        repositorio = new CobroCajaRepositoryImpl(jpa, mapper);
    }

    private CobroCaja guardado;
    private Optional<CobroCaja> encontrado;
    private List<CobroCaja> cobros;

    @Test
    @DisplayName("Guarda el cobro inmutable con ids, fecha y todas sus aplicaciones")
    void guardaElCobroInmutableConIdsFechaYTodasSusAplicaciones() {
        dadoQueGuardarDevuelveLaEntidadRecibida();

        cuandoSeGuardaElCobro();

        entoncesSeGuardaLaEntidadConIdsFechaYDatosDelCobro();
    }

    @Test
    @DisplayName("Busca cobros sólo dentro del negocio autenticado")
    void buscaCobrosSoloDentroDelNegocioAutenticado() {
        when(jpa.findByIdAndNegocioId(cobro.id().valor(), cobro.negocioId().valor()))
                .thenReturn(Optional.of(mapper.toEntity(cobro)));

        cuandoSeBuscaElCobroPorId();

        entoncesSeEncuentraElCobro();
    }

    @Test
    @DisplayName("Lista todos los pagos del inventario en su orden persistido")
    void listaTodosLosPagosDelInventarioEnSuOrdenPersistido() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnAscIdAsc(cobro.negocioId().valor(),
                cobro.proyectoId().valor(), cobro.inventarioId().valor())).thenReturn(List.of(mapper.toEntity(cobro)));

        cuandoSeListanTodosLosCobrosDelInventario();

        entoncesSeListaElCobro();
    }

    @Test
    @DisplayName("El resumen del proyecto incluye sus cobros de cualquier inventario y cierre")
    void elResumenDelProyectoIncluyeSusCobrosDeCualquierInventarioYCierre() {
        when(jpa.findByNegocioIdAndProyectoIdOrderByCreadoEnAscIdAsc(cobro.negocioId().valor(),
                cobro.proyectoId().valor())).thenReturn(List.of(mapper.toEntity(cobro)));

        cuandoSeListanLosCobrosDelProyecto();

        entoncesSeListaElCobro();
    }

    @Test
    @DisplayName("Lista sólo los cobros que todavía no tienen cierre")
    void listaSoloLosCobrosQueTodaviaNoTienenCierre() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdAndCierreIdIsNullOrderByCreadoEnAscIdAsc(
                cobro.negocioId().valor(), cobro.proyectoId().valor(), cobro.inventarioId().valor()))
                .thenReturn(List.of(mapper.toEntity(cobro)));

        cuandoSeListanLosCobrosAbiertos();

        entoncesSeListaElCobro();
    }

    @Test
    @DisplayName("El rango de fechas usa un inicio incluido y un fin excluido")
    void elRangoDeFechasUsaUnInicioIncluidoYUnFinExcluido() {
        dadoQueElRangoDeUnDiaTieneUnCobro();

        cuandoSeListanLosCobrosEntreHoyYManiana();

        entoncesSeListaElCobro();
    }

    @Test
    @DisplayName("El cierre vincula los ids del cobro sin alterar sus datos")
    void elCierreVinculaLosIdsDelCobroSinAlterarSusDatos() {
        cuandoSeMarcaElCobroComoCerrado();

        entoncesSePasaronElNegocioElCierreYLosIdsDelCobro();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveLaEntidadRecibida() {
        when(jpa.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoQueElRangoDeUnDiaTieneUnCobro() {
        when(jpa.findByNegocioIdAndProyectoIdAndInventarioIdAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnAscIdAsc(
                cobro.negocioId().valor(), cobro.proyectoId().valor(), cobro.inventarioId().valor(),
                CajaTestData.AHORA, CajaTestData.AHORA.plusDays(1))).thenReturn(List.of(mapper.toEntity(cobro)));
    }

    // --- cuando ---
    private void cuandoSeGuardaElCobro() {
        guardado = repositorio.save(cobro);
    }

    private void cuandoSeBuscaElCobroPorId() {
        encontrado = repositorio.findByIdAndNegocioId(cobro.id(), cobro.negocioId());
    }

    private void cuandoSeListanTodosLosCobrosDelInventario() {
        cobros = repositorio.listarTodos(cobro.negocioId(), cobro.proyectoId(), cobro.inventarioId());
    }

    private void cuandoSeListanLosCobrosDelProyecto() {
        cobros = repositorio.listarPorProyecto(cobro.negocioId(), cobro.proyectoId());
    }

    private void cuandoSeListanLosCobrosAbiertos() {
        cobros = repositorio.listarAbiertos(cobro.negocioId(), cobro.proyectoId(), cobro.inventarioId());
    }

    private void cuandoSeListanLosCobrosEntreHoyYManiana() {
        cobros = repositorio.listarEntre(cobro.negocioId(), cobro.proyectoId(), cobro.inventarioId(),
                CajaTestData.AHORA, CajaTestData.AHORA.plusDays(1));
    }

    private void cuandoSeMarcaElCobroComoCerrado() {
        repositorio.marcarCerrados(cobro.negocioId(), CierreCajaTestData.CIERRE, List.of(cobro.id()));
    }

    // --- entonces ---
    private void entoncesSeGuardaLaEntidadConIdsFechaYDatosDelCobro() {
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

    private void entoncesSeEncuentraElCobro() {
        assertThat(encontrado).contains(cobro);
    }

    private void entoncesSeListaElCobro() {
        assertThat(cobros).containsExactly(cobro);
    }

    private void entoncesSePasaronElNegocioElCierreYLosIdsDelCobro() {
        verify(jpa).marcarCerrados(cobro.negocioId().valor(), CierreCajaTestData.CIERRE.valor(),
                List.of(cobro.id().valor()));
    }
}
