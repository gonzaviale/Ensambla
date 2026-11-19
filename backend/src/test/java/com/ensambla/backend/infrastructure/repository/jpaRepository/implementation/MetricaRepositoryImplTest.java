package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMetricaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MetricaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.laFilaDeLasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricaRepositoryImplTest {

    private static final UUID OTRA_ID = UUID.fromString("e0000000-0000-4000-8000-000000000002");

    private final IMetricaJpaRepository jpa = mock(IMetricaJpaRepository.class);
    private final MetricaRepositoryImpl repository = new MetricaRepositoryImpl(jpa, new MetricaRepositoryMapper());

    private List<Metrica> metricas;

    private Optional<Metrica> encontrada;

    private Metrica guardada;

    @Test
    @DisplayName("Lista las métricas del cuaderno en el orden de la consulta y las lee al dominio")
    void listaLasMetricasEnElOrdenDeLaConsulta() {
        dadoUnCuadernoConFilas(laFilaDeLasVentasDelMes(METRICA_ID.valor(), 0), laFilaDeLasVentasDelMes(OTRA_ID, 1));

        cuandoSeListanLasDelCuaderno();

        entoncesSeListanEnEsteOrden(METRICA_ID, MetricaId.de(OTRA_ID));
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el cuaderno no tiene métricas")
    void devuelveUnaListaVaciaSinMetricas() {
        dadoUnCuadernoConFilas();

        cuandoSeListanLasDelCuaderno();

        entoncesSeListanEnEsteOrden();
    }

    @Test
    @DisplayName("Busca por id, cuaderno y negocio con los UUID y lee el resultado")
    void buscaPorIdCuadernoYNegocio() {
        dadaLaFilaDelCuaderno(Optional.of(laFilaDeLasVentasDelMes(METRICA_ID.valor(), 0)));

        cuandoSeBusca();

        entoncesSeEncuentra(METRICA_ID);
    }

    @Test
    @DisplayName("Devuelve vacío cuando la métrica no es del cuaderno")
    void devuelveVacioCuandoLaMetricaNoEsDelCuaderno() {
        dadaLaFilaDelCuaderno(Optional.empty());

        cuandoSeBusca();

        entoncesNoSeEncuentra();
    }

    @Test
    @DisplayName("Guarda la fila armada desde el dominio y devuelve lo persistido")
    void guardaLaFilaArmadaDesdeElDominio() {
        dadoQueLaBaseLeDaElIdYElLugar(METRICA_ID, 2);

        cuandoSeGuarda(Metrica.nueva(NEGOCIO_ID, PROYECTO_ID, lasVentasDelMesLlamadas("Ventas"), 2,
                AutoriaMetrica.PROPIA, AHORA));

        entoncesSeGuardoLaFilaDelNegocioLlamada("Ventas");
        entoncesSeDevuelveLaGuardadaCon(METRICA_ID);
    }

    @Test
    @DisplayName("Borra pasando al JPA los UUID de la métrica y del negocio")
    void borraPasandoLosUuid() {
        cuandoSeBorra();

        entoncesSeBorroConLosUuid();
    }

    private void dadoUnCuadernoConFilas(MetricaEntity... filas) {
        when(jpa.findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(filas));
    }

    private void dadaLaFilaDelCuaderno(Optional<MetricaEntity> fila) {
        when(jpa.findByIdAndProyectoIdAndNegocioId(METRICA_ID.valor(), PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(fila);
    }

    private void dadoQueLaBaseLeDaElIdYElLugar(MetricaId id, int orden) {
        when(jpa.save(any(MetricaEntity.class))).thenReturn(laFilaDeLasVentasDelMes(id.valor(), orden));
    }

    private void cuandoSeListanLasDelCuaderno() {
        metricas = repository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    private void cuandoSeBusca() {
        encontrada = repository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID);
    }

    private void cuandoSeGuarda(Metrica nueva) {
        guardada = repository.save(nueva);
    }

    private void cuandoSeBorra() {
        repository.deleteByIdAndNegocioId(METRICA_ID, NEGOCIO_ID);
    }

    private void entoncesSeListanEnEsteOrden(MetricaId... ids) {
        assertThat(metricas).extracting(Metrica::getId).containsExactly(ids);
    }

    private void entoncesSeEncuentra(MetricaId id) {
        assertThat(encontrada).map(Metrica::getId).contains(id);
    }

    private void entoncesNoSeEncuentra() {
        assertThat(encontrada).isEmpty();
    }

    private void entoncesSeGuardoLaFilaDelNegocioLlamada(String nombre) {
        ArgumentCaptor<MetricaEntity> captor = ArgumentCaptor.forClass(MetricaEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(captor.getValue().getProyectoId()).isEqualTo(PROYECTO_ID.valor());
        assertThat(captor.getValue().getNombre()).isEqualTo(nombre);
    }

    private void entoncesSeDevuelveLaGuardadaCon(MetricaId id) {
        assertThat(guardada.getId()).isEqualTo(id);
    }

    private void entoncesSeBorroConLosUuid() {
        verify(jpa).deleteByIdAndNegocioId(METRICA_ID.valor(), NEGOCIO_ID.valor());
    }
}
