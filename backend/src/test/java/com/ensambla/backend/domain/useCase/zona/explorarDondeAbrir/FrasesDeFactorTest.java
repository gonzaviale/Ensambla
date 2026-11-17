package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class FrasesDeFactorTest {

    private static final FrasesDeFactor.Medida MEDIDA = new FrasesDeFactor.Medida("3", "ARS", "kioscos");

    private String frase;

    @ParameterizedTest
    @EnumSource(FactorDondeAbrir.class)
    @DisplayName("Todo factor sabe decir por qué juega a favor de la zona")
    void aFavorShouldNotBeBlankWhenFactorIsAny(FactorDondeAbrir factor) {
        whenSePideLaFraseAFavorDe(factor);

        thenLaFraseTieneTexto();
    }

    @ParameterizedTest
    @EnumSource(FactorDondeAbrir.class)
    @DisplayName("Todo factor sabe decir por qué es un riesgo para la zona")
    void enContraShouldNotBeBlankWhenFactorIsAny(FactorDondeAbrir factor) {
        whenSePideLaFraseEnContraDe(factor);

        thenLaFraseTieneTexto();
    }

    @Test
    @DisplayName("La competencia nombra los comercios del rubro y la cantidad medida")
    void aFavorShouldMentionCompetitorsWhenFactorIsCompetition() {
        whenSePideLaFraseAFavorDe(FactorDondeAbrir.COMPETENCIA);

        thenLaFraseEs("Solo 3 kioscos a menos de 500 m");
    }

    @Test
    @DisplayName("La demanda de Ensambla muestra su unidad")
    void aFavorShouldMentionUnitWhenFactorIsEnsamblaDemand() {
        whenSePideLaFraseAFavorDe(FactorDondeAbrir.DEMANDA_ENSAMBLA);

        thenLaFraseEs("Demanda observada en comercios de Ensambla (3 ARS)");
    }

    @Test
    @DisplayName("Un riesgo de competencia advierte que es fuerte")
    void enContraShouldWarnAboutStrongCompetitionWhenFactorIsCompetition() {
        whenSePideLaFraseEnContraDe(FactorDondeAbrir.COMPETENCIA);

        thenLaFraseEs("Hay 3 kioscos a menos de 500 m: la competencia es fuerte");
    }

    private void whenSePideLaFraseAFavorDe(FactorDondeAbrir factor) {
        frase = FrasesDeFactor.aFavor(factor, MEDIDA);
    }

    private void whenSePideLaFraseEnContraDe(FactorDondeAbrir factor) {
        frase = FrasesDeFactor.enContra(factor, MEDIDA);
    }

    private void thenLaFraseTieneTexto() {
        assertThat(frase).isNotBlank();
    }

    private void thenLaFraseEs(String esperada) {
        assertThat(frase).isEqualTo(esperada);
    }
}
