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
    void todoFactorSabeDecirPorQueJuegaAFavorDeLaZona(FactorDondeAbrir factor) {
        cuandoSePideLaFraseAFavorDe(factor);

        entoncesLaFraseTieneTexto();
    }

    @ParameterizedTest
    @EnumSource(FactorDondeAbrir.class)
    @DisplayName("Todo factor sabe decir por qué es un riesgo para la zona")
    void todoFactorSabeDecirPorQueEsUnRiesgoParaLaZona(FactorDondeAbrir factor) {
        cuandoSePideLaFraseEnContraDe(factor);

        entoncesLaFraseTieneTexto();
    }

    @Test
    @DisplayName("La competencia nombra los comercios del rubro y la cantidad medida")
    void laCompetenciaNombraLosComerciosDelRubroYLaCantidadMedida() {
        cuandoSePideLaFraseAFavorDe(FactorDondeAbrir.COMPETENCIA);

        entoncesLaFraseEs("Solo 3 kioscos a menos de 500 m");
    }

    @Test
    @DisplayName("La demanda de Ensambla muestra su unidad")
    void laDemandaDeEnsamblaMuestraSuUnidad() {
        cuandoSePideLaFraseAFavorDe(FactorDondeAbrir.DEMANDA_ENSAMBLA);

        entoncesLaFraseEs("Demanda observada en comercios de Ensambla (3 ARS)");
    }

    @Test
    @DisplayName("Un riesgo de competencia advierte que es fuerte")
    void unRiesgoDeCompetenciaAdvierteQueEsFuerte() {
        cuandoSePideLaFraseEnContraDe(FactorDondeAbrir.COMPETENCIA);

        entoncesLaFraseEs("Hay 3 kioscos a menos de 500 m: la competencia es fuerte");
    }

    private void cuandoSePideLaFraseAFavorDe(FactorDondeAbrir factor) {
        frase = FrasesDeFactor.aFavor(factor, MEDIDA);
    }

    private void cuandoSePideLaFraseEnContraDe(FactorDondeAbrir factor) {
        frase = FrasesDeFactor.enContra(factor, MEDIDA);
    }

    private void entoncesLaFraseTieneTexto() {
        assertThat(frase).isNotBlank();
    }

    private void entoncesLaFraseEs(String esperada) {
        assertThat(frase).isEqualTo(esperada);
    }
}
