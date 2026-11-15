package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenSemanalTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);
    private static final ConteoDecisiones CONTEO = new ConteoDecisiones(1, 0, 2, 0);
    private static final CierreMini CIERRE = new CierreMini(MINI_ID, new BigDecimal("12600.00"));

    @Test
    @DisplayName("Arma el resumen con el avance hacia la meta y copias de las listas")
    void deShouldBuildTheSummaryWithTheProgressTowardsTheGoal() {
        List<CierreMini> cierres = new ArrayList<>(List.of(CIERRE));

        ResumenSemanal result = ResumenSemanal.de(objetivo(), SEMANA, new BigDecimal("100000"), CONTEO, cierres,
                "Esta semana…", List.of(), AHORA);
        cierres.clear();

        assertThat(result.id()).isNotNull();
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
        assertThat(result.resultado()).isEqualByComparingTo("100000");
        assertThat(result.avance()).isEqualByComparingTo("0.25");
        assertThat(result.decisiones()).isEqualTo(CONTEO);
        assertThat(result.minis()).containsExactly(CIERRE);
        assertThat(result.queFunciono()).isEqualTo("Esta semana…");
        assertThat(result.textoConIa()).isFalse();
        assertThat(result.cerradoEn()).isEqualTo(AHORA);
        assertThat(result.reajustes()).isEmpty();
    }

    @Test
    @DisplayName("El avance queda entre 0 y 1 y una meta de cero ya está cumplida")
    void avanceDeShouldStayBetweenZeroAndOne() {
        assertThat(ResumenSemanal.avanceDe(new BigDecimal("-500"), BigDecimal.TEN)).isEqualByComparingTo("0");
        assertThat(ResumenSemanal.avanceDe(new BigDecimal("500"), BigDecimal.TEN)).isEqualByComparingTo("1");
        assertThat(ResumenSemanal.avanceDe(new BigDecimal("500"), BigDecimal.ZERO)).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Da el valor de un mini-objetivo al cierre, o vacío si no estaba")
    void valorDeShouldReturnTheClosingValueOfAMiniObjetivo() {
        ResumenSemanal resumen = ResumenSemanal.de(objetivo(), SEMANA, BigDecimal.ZERO, CONTEO, List.of(CIERRE),
                "", List.of(), AHORA);

        assertThat(resumen.valorDe(MINI_ID)).contains(new BigDecimal("12600.00"));
        assertThat(resumen.valorDe(OTRO_MINI_ID)).isEmpty();
    }
}
