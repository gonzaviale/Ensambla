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
import static com.ensambla.backend.support.MetricaTestData.laFilaDeLoQueTeDeben;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricaTestData.unaFilaSinIds;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;

class MetricaRepositoryMapperTest {

    private static final DefinicionMetrica CUANTOS_GASTOS_POR_CATEGORIA = unaReceta("Gastos", FuenteMetrica.GASTOS,
            CalculoMetrica.CONTEO, AgrupacionMetrica.CATEGORIA, PeriodoMetrica.SEMANA, false);

    private static final DefinicionMetrica LAS_VENTAS_SIN_REPARTO_NI_PERIODO = unaReceta("Ventas",
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, null, null, false);

    private final MetricaRepositoryMapper mapper = new MetricaRepositoryMapper();

    private Metrica metrica;

    private MetricaEntity fila;

    @Test
    @DisplayName("Lee la fila guardada con todos sus datos")
    void leeLaFilaGuardadaConTodosSusDatos() {
        cuandoSeLee(laFilaDeLoQueTeDeben());

        entoncesLaMetricaEsLoQueTeDeben();
    }

    @Test
    @DisplayName("Una fila sin ids se lee con ids nulos")
    void unaFilaSinIdsSeLeeConIdsNulos() {
        cuandoSeLee(unaFilaSinIds());

        entoncesLaMetricaNoTieneIds();
    }

    @Test
    @DisplayName("Guarda la métrica en una fila con todos sus datos")
    void guardaLaMetricaEnUnaFilaConTodosSusDatos() {
        cuandoSeGuarda(metrica(CUANTOS_GASTOS_POR_CATEGORIA, 5));

        entoncesLaFilaEsLaDeCuantosGastosPorCategoria();
    }

    @Test
    @DisplayName("Una métrica nueva, sin id, se guarda sin id y con el reparto y el período por defecto")
    void unaMetricaNuevaSeGuardaSinIdYConLosValoresPorDefecto() {
        cuandoSeGuarda(Metrica.nueva(NEGOCIO_ID, PROYECTO_ID, LAS_VENTAS_SIN_REPARTO_NI_PERIODO, 0,
                AutoriaMetrica.PROPIA, AHORA));

        entoncesLaFilaEsNuevaSinRepartoYDelMes();
    }

    private void cuandoSeLee(MetricaEntity guardada) {
        metrica = mapper.toDomain(guardada);
    }

    private void cuandoSeGuarda(Metrica aGuardar) {
        fila = mapper.toEntity(aGuardar);
    }

    private void entoncesLaMetricaEsLoQueTeDeben() {
        assertThat(metrica.getId()).isEqualTo(METRICA_ID);
        assertThat(metrica.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(metrica.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(metrica.getDefinicion()).isEqualTo(unaReceta("Lo que te deben", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.CLIENTE, PeriodoMetrica.TODO, true));
        assertThat(metrica.getOrden()).isEqualTo(3);
        assertThat(metrica.getAutoria()).isEqualTo(AutoriaMetrica.PROPIA);
        assertThat(metrica.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaMetricaNoTieneIds() {
        assertThat(metrica.getId()).isNull();
        assertThat(metrica.getNegocioId()).isNull();
        assertThat(metrica.getProyectoId()).isNull();
    }

    private void entoncesLaFilaEsLaDeCuantosGastosPorCategoria() {
        assertThat(fila.getId()).isEqualTo(METRICA_ID.valor());
        assertThat(fila.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(fila.getProyectoId()).isEqualTo(PROYECTO_ID.valor());
        assertThat(fila.getNombre()).isEqualTo("Gastos");
        assertThat(fila.getFuente()).isEqualTo(FuenteMetrica.GASTOS);
        assertThat(fila.getCalculo()).isEqualTo(CalculoMetrica.CONTEO);
        assertThat(fila.getAgrupar()).isEqualTo(AgrupacionMetrica.CATEGORIA);
        assertThat(fila.getPeriodo()).isEqualTo(PeriodoMetrica.SEMANA);
        assertThat(fila.isSoloFiado()).isFalse();
        assertThat(fila.getOrden()).isEqualTo(5);
        assertThat(fila.getAutoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(fila.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaFilaEsNuevaSinRepartoYDelMes() {
        assertThat(fila.getId()).isNull();
        assertThat(fila.getAgrupar()).isEqualTo(AgrupacionMetrica.NINGUNA);
        assertThat(fila.getPeriodo()).isEqualTo(PeriodoMetrica.MES);
    }
}
