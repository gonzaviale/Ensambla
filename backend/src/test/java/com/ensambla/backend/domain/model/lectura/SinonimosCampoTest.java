package com.ensambla.backend.domain.model.lectura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SinonimosCampoTest {

    @Test
    @DisplayName("Limpia tildes, mayúsculas, puntuación y espacios de más")
    void limpiaTildesMayusculasPuntuacionYEspaciosDeMas() {
        entoncesLaLimpiezaDe("  Teléfono. ", "telefono");
        entoncesLaLimpiezaDe("Correo-Electrónico", "correo electronico");
    }

    @Test
    @DisplayName("Reconoce el mismo nombre escrito distinto")
    void reconoceElMismoNombreEscritoDistinto() {
        entoncesEsElMismoNombre("TEL.", " tel ", true);
        entoncesEsElMismoNombre("Tel", "Celular", false);
    }

    @Test
    @DisplayName("Reconoce nombres que suelen querer decir lo mismo")
    void reconoceNombresQueSuelenQuererDecirLoMismo() {
        entoncesSonEquivalentes("Teléfono", "Cel", true);
        entoncesSonEquivalentes("Importe", "TOTAL", true);
        entoncesSonEquivalentes("Correo electrónico", "Mail", true);
    }

    @Test
    @DisplayName("No mezcla grupos ni nombres que no están en la lista")
    void noMezclaGruposNiNombresQueNoEstanEnLaLista() {
        entoncesSonEquivalentes("Teléfono", "Dirección", false);
        entoncesSonEquivalentes("Teléfono", "Color", false);
        entoncesSonEquivalentes("Color", "Tono", false);
    }

    // --- entonces ---
    private void entoncesLaLimpiezaDe(String nombre, String esperado) {
        assertThat(SinonimosCampo.limpio(nombre)).isEqualTo(esperado);
    }

    private void entoncesEsElMismoNombre(String uno, String otro, boolean esperado) {
        assertThat(SinonimosCampo.mismoNombre(uno, otro)).isEqualTo(esperado);
    }

    private void entoncesSonEquivalentes(String uno, String otro, boolean esperado) {
        assertThat(SinonimosCampo.equivalentes(uno, otro)).isEqualTo(esperado);
    }
}
