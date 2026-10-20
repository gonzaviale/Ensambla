package com.ensambla.backend.domain.model.lectura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SinonimosCampoTest {

    @Test
    @DisplayName("Limpia tildes, mayúsculas, puntuación y espacios de más")
    void limpioShouldIgnoreAccentsCaseAndPunctuation() {
        assertThat(SinonimosCampo.limpio("  Teléfono. ")).isEqualTo("telefono");
        assertThat(SinonimosCampo.limpio("Correo-Electrónico")).isEqualTo("correo electronico");
    }

    @Test
    @DisplayName("Reconoce el mismo nombre escrito distinto")
    void mismoNombreShouldIgnoreFormat() {
        assertThat(SinonimosCampo.mismoNombre("TEL.", " tel ")).isTrue();
        assertThat(SinonimosCampo.mismoNombre("Tel", "Celular")).isFalse();
    }

    @Test
    @DisplayName("Reconoce nombres que suelen querer decir lo mismo")
    void equivalentesShouldMatchNamesOfTheSameGroup() {
        assertThat(SinonimosCampo.equivalentes("Teléfono", "Cel")).isTrue();
        assertThat(SinonimosCampo.equivalentes("Importe", "TOTAL")).isTrue();
        assertThat(SinonimosCampo.equivalentes("Correo electrónico", "Mail")).isTrue();
    }

    @Test
    @DisplayName("No mezcla grupos ni nombres que no están en la lista")
    void equivalentesShouldRejectDifferentGroupsAndUnknownNames() {
        assertThat(SinonimosCampo.equivalentes("Teléfono", "Dirección")).isFalse();
        assertThat(SinonimosCampo.equivalentes("Teléfono", "Color")).isFalse();
        assertThat(SinonimosCampo.equivalentes("Color", "Tono")).isFalse();
    }
}
