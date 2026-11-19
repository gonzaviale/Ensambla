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
    void armaElResumenConElAvanceHaciaLaMetaYCopiasDeLasListas() {
        dadoCierres();

        cuandoSeArmaElResumenConCierres(RESULTADO_DE_CIEN_MIL);
        cuandoSeVacianLosCierresOriginales();

        entoncesElResumenTieneElAvanceHaciaLaMeta();
    }

    @Test
    @DisplayName("Un resultado negativo deja el avance en 0")
    void unResultadoNegativoDejaElAvanceEn0() {
        cuandoSeCalculaElAvance(RESULTADO_NEGATIVO, BigDecimal.TEN);

        entoncesElAvanceEs("0");
    }

    @Test
    @DisplayName("Un resultado mayor que la meta deja el avance en 1")
    void unResultadoMayorQueLaMetaDejaElAvanceEn1() {
        cuandoSeCalculaElAvance(RESULTADO_DE_QUINIENTOS, BigDecimal.TEN);

        entoncesElAvanceEs("1");
    }

    @Test
    @DisplayName("Una meta de cero ya está cumplida")
    void unaMetaDeCeroYaEstaCumplida() {
        cuandoSeCalculaElAvance(RESULTADO_DE_QUINIENTOS, BigDecimal.ZERO);

        entoncesElAvanceEs("1");
    }

    @Test
    @DisplayName("Da el valor de un mini-objetivo al cierre")
    void daElValorDeUnMiniObjetivoAlCierre() {
        dadoCierres();
        cuandoSeArmaElResumenConCierres(BigDecimal.ZERO);

        cuandoSeBuscaElValorDe(MINI_ID);

        entoncesElValorEs(VALOR_DE_CIERRE);
    }

    @Test
    @DisplayName("Da vacío el valor de un mini-objetivo que no estaba al cierre")
    void daVacioElValorDeUnMiniObjetivoQueNoEstabaAlCierre() {
        dadoCierres();
        cuandoSeArmaElResumenConCierres(BigDecimal.ZERO);

        cuandoSeBuscaElValorDe(OTRO_MINI_ID);

        entoncesNoHayValor();
    }

    @Test
    @DisplayName("Sin meta en pesos el avance es cero")
    void sinMetaEnPesosElAvanceEsCero() {
        cuandoSeCalculaElAvanceSinMeta(RESULTADO_DE_QUINIENTOS);

        entoncesElAvanceEs("0");
    }

    @Test
    @DisplayName("El resumen guarda una copia de lo que la persona respondió de sus manuales")
    void elResumenGuardaUnaCopiaDeLoQueLaPersonaRespondioDeSusManuales() {
        dadoManuales();

        cuandoSeArmaElResumenConManuales();
        cuandoSeVacianLosManualesOriginales();

        entoncesElResumenGuardaLaRespuestaDelManual();
    }

    // --- arrange ---
    private void dadoCierres() {
        cierres = new ArrayList<>(List.of(CIERRE));
    }

    private void dadoManuales() {
        manuales = new ArrayList<>(List.of(new ManualCierre(MINI_ID, "Un cartel", HizoManual.SI, QuePaso.FUNCIONO)));
    }

    // --- act ---
    private void cuandoSeArmaElResumenConCierres(BigDecimal resultado) {
        resumen = ResumenSemanal.de(objetivo(), SEMANA, resultado, CONTEO, cierres, "Esta semana…", List.of(),
                List.of(), AHORA);
    }

    private void cuandoSeArmaElResumenConManuales() {
        resumen = ResumenSemanal.de(objetivo(), SEMANA, BigDecimal.ZERO, CONTEO, List.of(), "", List.of(), manuales,
                AHORA);
    }

    private void cuandoSeVacianLosCierresOriginales() {
        cierres.clear();
    }

    private void cuandoSeVacianLosManualesOriginales() {
        manuales.clear();
    }

    private void cuandoSeCalculaElAvance(BigDecimal resultado, BigDecimal meta) {
        avance = ResumenSemanal.avanceDe(resultado, meta);
    }

    private void cuandoSeCalculaElAvanceSinMeta(BigDecimal resultado) {
        avance = ResumenSemanal.avanceDe(resultado, null);
    }

    private void cuandoSeBuscaElValorDe(MiniObjetivoId id) {
        valor = resumen.valorDe(id);
    }

    // --- assert ---
    private void entoncesElResumenTieneElAvanceHaciaLaMeta() {
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

    private void entoncesElAvanceEs(String esperado) {
        assertThat(avance).isEqualByComparingTo(esperado);
    }

    private void entoncesElValorEs(BigDecimal esperado) {
        assertThat(valor).contains(esperado);
    }

    private void entoncesNoHayValor() {
        assertThat(valor).isEmpty();
    }

    private void entoncesElResumenGuardaLaRespuestaDelManual() {
        assertThat(resumen.manuales()).singleElement().satisfies(manual -> {
            assertThat(manual.titulo()).isEqualTo("Un cartel");
            assertThat(manual.hizo()).isEqualTo(HizoManual.SI);
        });
    }
}
