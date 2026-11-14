package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static org.assertj.core.api.Assertions.assertThat;

class MetricaRepositoryMapperTest {

    private final MetricaRepositoryMapper mapper = new MetricaRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void toDomainShouldMapEveryField() {
        MetricaEntity entity = MetricaEntity.builder()
                .id(METRICA_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .proyectoId(PROYECTO_ID.valor())
                .nombre("Lo que te deben")
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.CLIENTE)
                .periodo(PeriodoMetrica.TODO)
                .soloFiado(true)
                .orden(3)
                .autoria(AutoriaMetrica.PROPIA)
                .creadoEn(AHORA)
                .build();

        Metrica result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(METRICA_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getDefinicion().nombre()).isEqualTo("Lo que te deben");
        assertThat(result.getDefinicion().fuente()).isEqualTo(FuenteMetrica.VENTAS);
        assertThat(result.getDefinicion().calculo()).isEqualTo(CalculoMetrica.SUMA);
        assertThat(result.getDefinicion().agrupar()).isEqualTo(AgrupacionMetrica.CLIENTE);
        assertThat(result.getDefinicion().periodo()).isEqualTo(PeriodoMetrica.TODO);
        assertThat(result.getDefinicion().soloFiado()).isTrue();
        assertThat(result.getOrden()).isEqualTo(3);
        assertThat(result.getAutoria()).isEqualTo(AutoriaMetrica.PROPIA);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void toDomainShouldKeepMissingIdsNull() {
        Metrica result = mapper.toDomain(MetricaEntity.builder()
                .fuente(FuenteMetrica.GASTOS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.NINGUNA)
                .periodo(PeriodoMetrica.MES)
                .build());

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isNull();
        assertThat(result.getProyectoId()).isNull();
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        Metrica metrica = metrica(new DefinicionMetrica(
                "Gastos", FuenteMetrica.GASTOS, CalculoMetrica.CONTEO, AgrupacionMetrica.CATEGORIA,
                PeriodoMetrica.SEMANA, false), 5);

        MetricaEntity result = mapper.toEntity(metrica);

        assertThat(result.getId()).isEqualTo(METRICA_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID.valor());
        assertThat(result.getNombre()).isEqualTo("Gastos");
        assertThat(result.getFuente()).isEqualTo(FuenteMetrica.GASTOS);
        assertThat(result.getCalculo()).isEqualTo(CalculoMetrica.CONTEO);
        assertThat(result.getAgrupar()).isEqualTo(AgrupacionMetrica.CATEGORIA);
        assertThat(result.getPeriodo()).isEqualTo(PeriodoMetrica.SEMANA);
        assertThat(result.isSoloFiado()).isFalse();
        assertThat(result.getOrden()).isEqualTo(5);
        assertThat(result.getAutoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Una métrica nueva, sin id, se mapea con id nulo")
    void toEntityShouldKeepMissingIdNull() {
        DefinicionMetrica definicion = new DefinicionMetrica(
                "Ventas", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, null, null, false);
        Metrica nueva = Metrica.nueva(NEGOCIO_ID, PROYECTO_ID, definicion, 0, AutoriaMetrica.PROPIA, AHORA);

        MetricaEntity result = mapper.toEntity(nueva);

        assertThat(result.getId()).isNull();
        assertThat(result.getAgrupar()).isEqualTo(AgrupacionMetrica.NINGUNA);
        assertThat(result.getPeriodo()).isEqualTo(PeriodoMetrica.MES);
    }
}
