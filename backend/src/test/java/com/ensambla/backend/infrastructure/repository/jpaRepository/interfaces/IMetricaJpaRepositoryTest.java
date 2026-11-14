package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IMetricaJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private IMetricaJpaRepository metricas;

    private UUID negocioA;
    private UUID negocioB;
    private UUID proyectoA;
    private UUID otroProyectoA;
    private UUID proyectoB;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        proyectoA = givenProyecto(negocioA, "Almacén");
        otroProyectoA = givenProyecto(negocioA, "Kiosco");
        proyectoB = givenProyecto(negocioB, "Ajeno");
    }

    @Test
    @DisplayName("Guarda y relee la métrica con todos sus datos")
    void saveShouldRoundTripEveryField() {
        MetricaEntity guardada = metricas.saveAndFlush(MetricaEntity.builder()
                .negocioId(negocioA)
                .proyectoId(proyectoA)
                .nombre("Lo que te deben")
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.CLIENTE)
                .periodo(PeriodoMetrica.TODO)
                .soloFiado(true)
                .orden(4)
                .autoria(AutoriaMetrica.PROPIA)
                .creadoEn(AHORA)
                .build());

        MetricaEntity result = metricas.findByIdAndProyectoIdAndNegocioId(guardada.getId(), proyectoA, negocioA)
                .orElseThrow();

        assertThat(result.getNombre()).isEqualTo("Lo que te deben");
        assertThat(result.getFuente()).isEqualTo(FuenteMetrica.VENTAS);
        assertThat(result.getCalculo()).isEqualTo(CalculoMetrica.SUMA);
        assertThat(result.getAgrupar()).isEqualTo(AgrupacionMetrica.CLIENTE);
        assertThat(result.getPeriodo()).isEqualTo(PeriodoMetrica.TODO);
        assertThat(result.isSoloFiado()).isTrue();
        assertThat(result.getOrden()).isEqualTo(4);
        assertThat(result.getAutoria()).isEqualTo(AutoriaMetrica.PROPIA);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Asigna un id cuando la entidad no trae uno")
    void saveShouldGenerateIdWhenMissing() {
        assertThat(givenMetrica(negocioA, proyectoA, "Sin id", 0).getId()).isNotNull();
    }

    @Test
    @DisplayName("Conserva el id cuando la entidad ya trae uno")
    void saveShouldKeepProvidedId() {
        UUID id = UUID.fromString("00000000-0000-4000-8000-0000000000b1");
        MetricaEntity entidad = metrica(negocioA, proyectoA, "Con id", 0);
        entidad.setId(id);

        assertThat(metricas.saveAndFlush(entidad).getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Lista sólo las métricas del cuaderno y del negocio, en el orden del panel")
    void findAllShouldFilterByProjectAndNegocioAndOrder() {
        UUID segunda = givenMetrica(negocioA, proyectoA, "Segunda", 1).getId();
        UUID primera = givenMetrica(negocioA, proyectoA, "Primera", 0).getId();
        givenMetrica(negocioA, otroProyectoA, "De otro cuaderno", 0);
        givenMetrica(negocioB, proyectoB, "Ajena", 0);

        List<MetricaEntity> result = metricas.findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(proyectoA, negocioA);

        assertThat(result).extracting(MetricaEntity::getId).containsExactly(primera, segunda);
    }

    @Test
    @DisplayName("Un cuaderno ajeno no ve las métricas aunque se pida con su id")
    void findAllShouldNotLeakAcrossNegocios() {
        givenMetrica(negocioA, proyectoA, "Propia", 0);

        assertThat(metricas.findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(proyectoA, negocioB)).isEmpty();
    }

    @Test
    @DisplayName("Una métrica de otro negocio o de otro cuaderno no se encuentra")
    void findByIdShouldNotFindForeignMetrics() {
        UUID id = givenMetrica(negocioA, proyectoA, "Propia", 0).getId();

        assertThat(metricas.findByIdAndProyectoIdAndNegocioId(id, proyectoA, negocioB)).isEmpty();
        assertThat(metricas.findByIdAndProyectoIdAndNegocioId(id, otroProyectoA, negocioA)).isEmpty();
    }

    @Test
    @DisplayName("Borra sólo la métrica del negocio indicado")
    void deleteShouldOnlyRemoveTheMetricOfTheNegocio() {
        UUID propia = givenMetrica(negocioA, proyectoA, "Propia", 0).getId();
        UUID ajena = givenMetrica(negocioB, proyectoB, "Ajena", 0).getId();

        metricas.deleteByIdAndNegocioId(ajena, negocioA);
        metricas.deleteByIdAndNegocioId(propia, negocioA);

        assertThat(metricas.findById(propia)).isEmpty();
        assertThat(metricas.findById(ajena)).isPresent();
    }

    @Test
    @DisplayName("La base rechaza un lugar negativo en el panel")
    void saveShouldRejectNegativeOrder() {
        assertThatThrownBy(() -> metricas.saveAndFlush(metrica(negocioA, proyectoA, "Mal", -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---
    private UUID givenProyecto(UUID negocioId, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId)
                .nombre(nombre)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private MetricaEntity givenMetrica(UUID negocioId, UUID proyectoId, String nombre, int orden) {
        return metricas.saveAndFlush(metrica(negocioId, proyectoId, nombre, orden));
    }

    private MetricaEntity metrica(UUID negocioId, UUID proyectoId, String nombre, int orden) {
        return MetricaEntity.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre)
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.NINGUNA)
                .periodo(PeriodoMetrica.MES)
                .orden(orden)
                .autoria(AutoriaMetrica.SUGERIDA)
                .creadoEn(AHORA)
                .build();
    }
}
