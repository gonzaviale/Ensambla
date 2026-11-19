package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatosDelNegocioTest {

    private static final BigDecimal META = new BigDecimal("500000.00");

    private CandidatosDelNegocio negocio;
    private boolean alcanza;

    @Test
    @DisplayName("Alcanza la meta cuando el resultado la iguala")
    void alcanzaLaMetaCuandoElResultadoLaIguala() {
        dadoUnNegocioConResultado("500000");

        cuandoSeComparaConLaMeta();

        entoncesAlcanzaLaMeta();
    }

    @Test
    @DisplayName("Alcanza la meta cuando el resultado la pasa")
    void alcanzaLaMetaCuandoElResultadoLaPasa() {
        dadoUnNegocioConResultado("620000");

        cuandoSeComparaConLaMeta();

        entoncesAlcanzaLaMeta();
    }

    @Test
    @DisplayName("No alcanza la meta cuando el resultado queda por debajo")
    void noAlcanzaLaMetaCuandoElResultadoQuedaPorDebajo() {
        dadoUnNegocioConResultado("499999");

        cuandoSeComparaConLaMeta();

        entoncesNoAlcanzaLaMeta();
    }

    // --- dado ---

    private void dadoUnNegocioConResultado(String resultado) {
        negocio = new CandidatosDelNegocio(new BigDecimal(resultado), List.of());
    }

    // --- cuando ---

    private void cuandoSeComparaConLaMeta() {
        alcanza = negocio.alcanza(META);
    }

    // --- entonces ---

    private void entoncesAlcanzaLaMeta() {
        assertThat(alcanza).isTrue();
    }

    private void entoncesNoAlcanzaLaMeta() {
        assertThat(alcanza).isFalse();
    }
}
