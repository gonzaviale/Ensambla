package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenSemanalTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);
    private static final ConteoDecisiones CONTEO = new ConteoDecisiones(1, 0, 2, 0);
    private static final BigDecimal VALOR_DE_CIERRE = new BigDecimal("12600.00");
    private static final BigDecimal RESULTADO_DE_CIEN_MIL = new BigDecimal("100000");
    private static final BigDecimal RESULTADO_NEGATIVO = new BigDecimal("-500");
    private static final BigDecimal RESULTADO_DE_QUINIENTOS = new BigDecimal("500");
    private static final CierreMini CIERRE = new CierreMini(MINI_ID, VALOR_DE_CIERRE);

    private List<CierreMini> cierres;
    private List<ManualCierre> manuales;
    private ResumenSemanal resumen;
    private BigDecimal avance;
    private Optional<BigDecimal> valor;

    @Test
    @DisplayName("Arma el resumen con el avance hacia la meta y copias de las listas")
    void deShouldBuildTheSummaryWithTheProgressTowardsTheGoal() {
        givenCierres();

        whenSeArmaElResumenConCierres(RESULTADO_DE_CIEN_MIL);
        whenSeVacianLosCierresOriginales();

        thenElResumenTieneElAvanceHaciaLaMeta();
    }

    @Test
    @DisplayName("Un resultado negativo deja el avance en 0")
    void avanceDeShouldNotBeBelowZero() {
        whenSeCalculaElAvance(RESULTADO_NEGATIVO, BigDecimal.TEN);

        thenElAvanceEs("0");
    }

    @Test
    @DisplayName("Un resultado mayor que la meta deja el avance en 1")
    void avanceDeShouldNotBeAboveOne() {
        whenSeCalculaElAvance(RESULTADO_DE_QUINIENTOS, BigDecimal.TEN);

        thenElAvanceEs("1");
    }

    @Test
    @DisplayName("Una meta de cero ya está cumplida")
    void avanceDeShouldBeOneForAZeroGoal() {
        whenSeCalculaElAvance(RESULTADO_DE_QUINIENTOS, BigDecimal.ZERO);

        thenElAvanceEs("1");
    }

    @Test
    @DisplayName("Da el valor de un mini-objetivo al cierre")
    void valorDeShouldReturnTheClosingValueOfAMiniObjetivo() {
        givenCierres();
        whenSeArmaElResumenConCierres(BigDecimal.ZERO);

        whenSeBuscaElValorDe(MINI_ID);

        thenElValorEs(VALOR_DE_CIERRE);
    }

    @Test
    @DisplayName("Da vacío el valor de un mini-objetivo que no estaba al cierre")
    void valorDeShouldBeEmptyForAMiniObjetivoThatWasNotThere() {
        givenCierres();
        whenSeArmaElResumenConCierres(BigDecimal.ZERO);

        whenSeBuscaElValorDe(OTRO_MINI_ID);

        thenNoHayValor();
    }

    @Test
    @DisplayName("Sin meta en pesos el avance es cero")
    void avanceDeShouldBeZeroWithoutGoal() {
        whenSeCalculaElAvanceSinMeta(RESULTADO_DE_QUINIENTOS);

        thenElAvanceEs("0");
    }

    @Test
    @DisplayName("El resumen guarda una copia de lo que la persona respondió de sus manuales")
    void deShouldKeepACopyOfTheManualAnswers() {
        givenManuales();

        whenSeArmaElResumenConManuales();
        whenSeVacianLosManualesOriginales();

        thenElResumenGuardaLaRespuestaDelManual();
    }

    // --- arrange ---
    private void givenCierres() {
        cierres = new ArrayList<>(List.of(CIERRE));
    }

    private void givenManuales() {
        manuales = new ArrayList<>(List.of(new ManualCierre(MINI_ID, "Un cartel", HizoManual.SI, QuePaso.FUNCIONO)));
    }

    // --- act ---
    private void whenSeArmaElResumenConCierres(BigDecimal resultado) {
        resumen = ResumenSemanal.de(objetivo(), SEMANA, resultado, CONTEO, cierres, "Esta semana…", List.of(),
                List.of(), AHORA);
    }

    private void whenSeArmaElResumenConManuales() {
        resumen = ResumenSemanal.de(objetivo(), SEMANA, BigDecimal.ZERO, CONTEO, List.of(), "", List.of(), manuales,
                AHORA);
    }

    private void whenSeVacianLosCierresOriginales() {
        cierres.clear();
    }

    private void whenSeVacianLosManualesOriginales() {
        manuales.clear();
    }

    private void whenSeCalculaElAvance(BigDecimal resultado, BigDecimal meta) {
        avance = ResumenSemanal.avanceDe(resultado, meta);
    }

    private void whenSeCalculaElAvanceSinMeta(BigDecimal resultado) {
        avance = ResumenSemanal.avanceDe(resultado, null);
    }

    private void whenSeBuscaElValorDe(MiniObjetivoId id) {
        valor = resumen.valorDe(id);
    }

    // --- assert ---
    private void thenElResumenTieneElAvanceHaciaLaMeta() {
        assertThat(resumen.id()).isNotNull();
        assertThat(resumen.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(resumen.semanaInicio()).isEqualTo(SEMANA);
        assertThat(resumen.resultado()).isEqualByComparingTo("100000");
        assertThat(resumen.avance()).isEqualByComparingTo("0.25");
        assertThat(resumen.decisiones()).isEqualTo(CONTEO);
        assertThat(resumen.minis()).containsExactly(CIERRE);
        assertThat(resumen.queFunciono()).isEqualTo("Esta semana…");
        assertThat(resumen.textoConIa()).isFalse();
        assertThat(resumen.cerradoEn()).isEqualTo(AHORA);
        assertThat(resumen.reajustes()).isEmpty();
    }

    private void thenElAvanceEs(String esperado) {
        assertThat(avance).isEqualByComparingTo(esperado);
    }

    private void thenElValorEs(BigDecimal esperado) {
        assertThat(valor).contains(esperado);
    }

    private void thenNoHayValor() {
        assertThat(valor).isEmpty();
    }

    private void thenElResumenGuardaLaRespuestaDelManual() {
        assertThat(resumen.manuales()).singleElement().satisfies(manual -> {
            assertThat(manual.titulo()).isEqualTo("Un cartel");
            assertThat(manual.hizo()).isEqualTo(HizoManual.SI);
        });
    }
}
