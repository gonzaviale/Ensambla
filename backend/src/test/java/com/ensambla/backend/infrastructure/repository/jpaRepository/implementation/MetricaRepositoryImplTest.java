package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricaRepositoryImplTest {

    private static final UUID OTRA_ID = UUID.fromString("e0000000-0000-4000-8000-000000000002");

    private final IMetricaJpaRepository jpa = mock(IMetricaJpaRepository.class);
    private final MetricaRepositoryImpl repository = new MetricaRepositoryImpl(jpa, new MetricaRepositoryMapper());

    @Test
    @DisplayName("Lista las métricas del cuaderno en el orden de la consulta y las mapea al dominio")
    void findAllShouldMapInOrder() {
        when(jpa.findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(entity(METRICA_ID.valor(), 0), entity(OTRA_ID, 1)));

        List<Metrica> result = repository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);

        assertThat(result).extracting(metrica -> metrica.getId().valor()).containsExactly(METRICA_ID.valor(), OTRA_ID);
        assertThat(result.get(1).getOrden()).isEqualTo(1);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el cuaderno no tiene métricas")
    void findAllShouldReturnEmpty() {
        when(jpa.findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of());

        assertThat(repository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Busca por id, cuaderno y negocio con los UUID y mapea el resultado")
    void findByIdShouldMapPresent() {
        when(jpa.findByIdAndProyectoIdAndNegocioId(METRICA_ID.valor(), PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(entity(METRICA_ID.valor(), 0)));

        Optional<Metrica> result = repository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID);

        assertThat(result).map(Metrica::getId).contains(METRICA_ID);
    }

    @Test
    @DisplayName("Devuelve vacío cuando la métrica no es del cuaderno")
    void findByIdShouldReturnEmptyWhenAbsent() {
        when(jpa.findByIdAndProyectoIdAndNegocioId(METRICA_ID.valor(), PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());

        assertThat(repository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Guarda la entidad armada desde el dominio y devuelve lo persistido")
    void saveShouldPersistAndReturnSaved() {
        when(jpa.save(any(MetricaEntity.class))).thenReturn(entity(METRICA_ID.valor(), 2));
        Metrica nueva = Metrica.builder()
                .negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID)
                .definicion(new DefinicionMetrica("Ventas", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                        AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES, false))
                .orden(2)
                .autoria(AutoriaMetrica.PROPIA)
                .creadoEn(AHORA)
                .build();

        Metrica result = repository.save(nueva);

        ArgumentCaptor<MetricaEntity> captor = ArgumentCaptor.forClass(MetricaEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(captor.getValue().getProyectoId()).isEqualTo(PROYECTO_ID.valor());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ventas");
        assertThat(result.getId()).isEqualTo(METRICA_ID);
    }

    @Test
    @DisplayName("Borra pasando al JPA los UUID de la métrica y del negocio")
    void deleteShouldPassUuids() {
        repository.deleteByIdAndNegocioId(METRICA_ID, NEGOCIO_ID);

        verify(jpa).deleteByIdAndNegocioId(METRICA_ID.valor(), NEGOCIO_ID.valor());
    }

    // --- helpers ---
    private static MetricaEntity entity(UUID id, int orden) {
        return MetricaEntity.builder()
                .id(id)
                .negocioId(NEGOCIO_ID.valor())
                .proyectoId(PROYECTO_ID.valor())
                .nombre("Ventas")
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.NINGUNA)
                .periodo(PeriodoMetrica.MES)
                .orden(orden)
                .autoria(AutoriaMetrica.PROPIA)
                .creadoEn(AHORA)
                .build();
    }
}
