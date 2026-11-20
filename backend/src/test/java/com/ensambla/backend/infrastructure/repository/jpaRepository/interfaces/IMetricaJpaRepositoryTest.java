package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.support.MetricaTestData;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.MetricaTestData.laFilaDeLoQueTeDebenEn;
import static com.ensambla.backend.support.MetricaTestData.unaFilaSugeridaEn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IMetricaJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final UUID UN_ID_ELEGIDO = UUID.fromString("00000000-0000-4000-8000-0000000000b1");

    @Autowired
    private IMetricaJpaRepository metricas;

    private UUID negocioA;
    private UUID negocioB;
    private UUID cuadernoA;
    private UUID otroCuadernoA;
    private UUID cuadernoB;

    private MetricaEntity guardada;

    private Optional<MetricaEntity> encontrada;

    private List<MetricaEntity> listadas;

    private Throwable rechazo;

    @BeforeEach
    void dadosDosNegociosConSusCuadernos() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        cuadernoA = unCuaderno(negocioA, "Almacén");
        otroCuadernoA = unCuaderno(negocioA, "Kiosco");
        cuadernoB = unCuaderno(negocioB, "Ajeno");
    }

    @Test
    @DisplayName("Guarda y relee la métrica con todos sus datos")
    void guardaYReleeLaMetricaConTodosSusDatos() {
        dadaGuardada(laFilaDeLoQueTeDebenEn(negocioA, cuadernoA));

        cuandoSeBuscaEn(guardada.getId(), cuadernoA, negocioA);

        entoncesSeReleeLoQueTeDeben();
    }

    @Test
    @DisplayName("Asigna un id cuando la fila no trae uno")
    void asignaUnIdCuandoLaFilaNoTraeUno() {
        cuandoSeGuarda(unaFilaSugeridaEn(negocioA, cuadernoA, "Sin id", 0));

        entoncesTieneId();
    }

    @Test
    @DisplayName("Conserva el id cuando la fila ya trae uno")
    void conservaElIdCuandoLaFilaYaTraeUno() {
        cuandoSeGuarda(conId(UN_ID_ELEGIDO, unaFilaSugeridaEn(negocioA, cuadernoA, "Con id", 0)));

        entoncesTieneElId(UN_ID_ELEGIDO);
    }

    @Test
    @DisplayName("Lista sólo las métricas del cuaderno y del negocio, en el orden del panel")
    void listaSoloLasDelCuadernoYDelNegocioEnOrden() {
        UUID segunda = dadaGuardada(unaFilaSugeridaEn(negocioA, cuadernoA, "Segunda", 1));
        UUID primera = dadaGuardada(unaFilaSugeridaEn(negocioA, cuadernoA, "Primera", 0));
        dadaGuardada(unaFilaSugeridaEn(negocioA, otroCuadernoA, "De otro cuaderno", 0));
        dadaGuardada(unaFilaSugeridaEn(negocioB, cuadernoB, "Ajena", 0));

        cuandoSeListan(cuadernoA, negocioA);

        entoncesSeListanEnEsteOrden(primera, segunda);
    }

    @Test
    @DisplayName("Un negocio ajeno no ve las métricas aunque pida el cuaderno por su id")
    void unNegocioAjenoNoVeLasMetricas() {
        dadaGuardada(unaFilaSugeridaEn(negocioA, cuadernoA, "Propia", 0));

        cuandoSeListan(cuadernoA, negocioB);

        entoncesSeListanEnEsteOrden();
    }

    @Test
    @DisplayName("Una métrica de otro negocio no se encuentra")
    void unaMetricaDeOtroNegocioNoSeEncuentra() {
        UUID propia = dadaGuardada(unaFilaSugeridaEn(negocioA, cuadernoA, "Propia", 0));

        cuandoSeBuscaEn(propia, cuadernoA, negocioB);

        entoncesNoSeEncuentra();
    }

    @Test
    @DisplayName("Una métrica de otro cuaderno no se encuentra")
    void unaMetricaDeOtroCuadernoNoSeEncuentra() {
        UUID propia = dadaGuardada(unaFilaSugeridaEn(negocioA, cuadernoA, "Propia", 0));

        cuandoSeBuscaEn(propia, otroCuadernoA, negocioA);

        entoncesNoSeEncuentra();
    }

    @Test
    @DisplayName("Borra la métrica del negocio indicado")
    void borraLaMetricaDelNegocioIndicado() {
        UUID propia = dadaGuardada(unaFilaSugeridaEn(negocioA, cuadernoA, "Propia", 0));

        cuandoSeBorra(propia, negocioA);

        entoncesNoExiste(propia);
    }

    @Test
    @DisplayName("No borra la métrica de otro negocio")
    void noBorraLaMetricaDeOtroNegocio() {
        UUID ajena = dadaGuardada(unaFilaSugeridaEn(negocioB, cuadernoB, "Ajena", 0));

        cuandoSeBorra(ajena, negocioA);

        entoncesExiste(ajena);
    }

    @Test
    @DisplayName("Guarda y relee una métrica de ganancia")
    void guardaYReleeUnaMetricaDeGanancia() {
        MetricaEntity fila = unaFilaSugeridaEn(negocioA, cuadernoA, "Ganancia", 0);
        fila.setCalculo(CalculoMetrica.GANANCIA);
        dadaGuardada(fila);

        cuandoSeBuscaEn(guardada.getId(), cuadernoA, negocioA);

        assertThat(encontrada).get().extracting(MetricaEntity::getCalculo).isEqualTo(CalculoMetrica.GANANCIA);
    }

    @Test
    @DisplayName("La base rechaza un lugar negativo en el panel")
    void laBaseRechazaUnLugarNegativo() {
        cuandoSeIntentaGuardar(unaFilaSugeridaEn(negocioA, cuadernoA, "Mal", -1));

        entoncesLaBaseLaRechaza();
    }

    private UUID unCuaderno(UUID negocioId, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId)
                .nombre(nombre)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private static MetricaEntity conId(UUID id, MetricaEntity fila) {
        fila.setId(id);
        return fila;
    }

    private UUID dadaGuardada(MetricaEntity fila) {
        cuandoSeGuarda(fila);
        return guardada.getId();
    }

    private void cuandoSeGuarda(MetricaEntity fila) {
        guardada = metricas.saveAndFlush(fila);
    }

    private void cuandoSeIntentaGuardar(MetricaEntity fila) {
        rechazo = catchThrowable(() -> cuandoSeGuarda(fila));
    }

    private void cuandoSeBuscaEn(UUID id, UUID proyectoId, UUID negocioId) {
        encontrada = metricas.findByIdAndProyectoIdAndNegocioId(id, proyectoId, negocioId);
    }

    private void cuandoSeListan(UUID proyectoId, UUID negocioId) {
        listadas = metricas.findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(proyectoId, negocioId);
    }

    private void cuandoSeBorra(UUID id, UUID negocioId) {
        metricas.deleteByIdAndNegocioId(id, negocioId);
    }

    private void entoncesSeReleeLoQueTeDeben() {
        MetricaEntity releida = encontrada.orElseThrow();
        assertThat(releida.getNombre()).isEqualTo("Lo que te deben");
        assertThat(releida.getFuente()).isEqualTo(FuenteMetrica.VENTAS);
        assertThat(releida.getCalculo()).isEqualTo(CalculoMetrica.SUMA);
        assertThat(releida.getAgrupar()).isEqualTo(AgrupacionMetrica.CLIENTE);
        assertThat(releida.getPeriodo()).isEqualTo(PeriodoMetrica.TODO);
        assertThat(releida.isSoloFiado()).isTrue();
        assertThat(releida.getOrden()).isEqualTo(3);
        assertThat(releida.getAutoria()).isEqualTo(AutoriaMetrica.PROPIA);
        assertThat(releida.getCreadoEn()).isAtSameInstantAs(MetricaTestData.AHORA);
    }

    private void entoncesTieneId() {
        assertThat(guardada.getId()).isNotNull();
    }

    private void entoncesTieneElId(UUID id) {
        assertThat(guardada.getId()).isEqualTo(id);
    }

    private void entoncesSeListanEnEsteOrden(UUID... ids) {
        assertThat(listadas).extracting(MetricaEntity::getId).containsExactly(ids);
    }

    private void entoncesNoSeEncuentra() {
        assertThat(encontrada).isEmpty();
    }

    private void entoncesNoExiste(UUID id) {
        assertThat(metricas.findById(id)).isEmpty();
    }

    private void entoncesExiste(UUID id) {
        assertThat(metricas.findById(id)).isPresent();
    }

    private void entoncesLaBaseLaRechaza() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }
}
