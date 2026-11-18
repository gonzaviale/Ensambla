package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextoNormalizadoTest {

    private static final String LO_QUE_ESCRIBIO_LA_PERSONA = "anoto quien me debe plata";

    private String normalizado;
    private List<String> palabras;
    private List<String> palabrasDelSingular;
    private boolean contiene;
    private String clave;
    private String claveDelSingular;

    @Test
    @DisplayName("Ignora tildes, mayúsculas y espacios de los bordes")
    void ignoraTildesMayusculasYEspaciosDeLosBordes() {
        cuandoSeNormaliza("  Juan PÉREZ ");

        entoncesElTextoNormalizadoEs("juan perez");
    }

    @Test
    @DisplayName("Conserva los espacios y signos del medio")
    void conservaLosEspaciosYSignosDelMedio() {
        cuandoSeNormaliza("Ñoquis  de papa-2");

        entoncesElTextoNormalizadoEs("noquis  de papa-2");
    }

    @Test
    @DisplayName("Pasa a minúsculas, quita tildes y signos y separa en palabras")
    void pasaAMinusculasQuitaTildesYSignosYSeparaEnPalabras() {
        cuandoSeSeparanEnPalabras("  ¡Cobro, ANOTO: quién me DEBE!  ");

        entoncesLasPalabrasSon("cobr", "anot", "quien", "me", "debe");
    }

    @ParameterizedTest
    @CsvSource({"velas,vela", "clientes,cliente", "pedidos,pedido"})
    @DisplayName("El singular y el plural dan la misma raíz")
    void elSingularYElPluralDanLaMismaRaiz(String plural, String singular) {
        cuandoSeSeparanEnPalabrasElPluralYElSingular(plural, singular);

        entoncesLasPalabrasDelPluralYDelSingularSonLasMismas();
    }

    @Test
    @DisplayName("No recorta las palabras cortas ni las que no terminan en vocal o plural")
    void noRecortaLasPalabrasCortasNiLasQueNoTerminanEnVocalOPlural() {
        cuandoSeSeparanEnPalabras("me los dos stock luz");

        entoncesLasPalabrasSon("me", "los", "dos", "stock", "luz");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "¿¡...!?"})
    @DisplayName("Un texto vacío o sólo de signos no tiene palabras")
    void unTextoVacioOSoloDeSignosNoTienePalabras(String texto) {
        cuandoSeSeparanEnPalabras(texto);

        entoncesNoHayPalabras();
    }

    @Test
    @DisplayName("Encuentra una frase completa y en orden dentro de las palabras")
    void encuentraUnaFraseCompletaYEnOrdenDentroDeLasPalabras() {
        cuandoSeBuscaLaFraseEnLoQueEscribioLaPersona("me debe");

        entoncesLaFraseSeEncuentra();
    }

    @Test
    @DisplayName("No encuentra una frase cuyas palabras están en otro orden")
    void noEncuentraUnaFraseCuyasPalabrasEstanEnOtroOrden() {
        cuandoSeBuscaLaFraseEnLoQueEscribioLaPersona("debe me");

        entoncesLaFraseNoSeEncuentra();
    }

    @Test
    @DisplayName("No encuentra una frase que la persona no terminó de escribir")
    void noEncuentraUnaFraseQueLaPersonaNoTerminoDeEscribir() {
        cuandoSeBuscaLaFraseEnLoQueEscribioLaPersona("me deben mucho");

        entoncesLaFraseNoSeEncuentra();
    }

    @Test
    @DisplayName("Una frase vacía nunca coincide")
    void unaFraseVaciaNuncaCoincide() {
        cuandoSeBuscaUnaFraseVaciaEnUnaPalabra();

        entoncesLaFraseNoSeEncuentra();
    }

    @Test
    @DisplayName("La clave de un nombre ignora tildes, mayúsculas y plural")
    void laClaveDeUnNombreIgnoraTildesMayusculasYPlural() {
        cuandoSeArmanLasClavesDe("Teléfonos", "telefono");

        entoncesLasClavesSonIguales();
    }

    // --- cuando ---
    private void cuandoSeNormaliza(String texto) {
        normalizado = TextoNormalizado.de(texto);
    }

    private void cuandoSeSeparanEnPalabras(String texto) {
        palabras = TextoNormalizado.palabras(texto);
    }

    private void cuandoSeSeparanEnPalabrasElPluralYElSingular(String plural, String singular) {
        palabras = TextoNormalizado.palabras(plural);
        palabrasDelSingular = TextoNormalizado.palabras(singular);
    }

    private void cuandoSeBuscaLaFraseEnLoQueEscribioLaPersona(String frase) {
        List<String> escrito = TextoNormalizado.palabras(LO_QUE_ESCRIBIO_LA_PERSONA);
        contiene = TextoNormalizado.contiene(escrito, TextoNormalizado.palabras(frase));
    }

    private void cuandoSeBuscaUnaFraseVaciaEnUnaPalabra() {
        contiene = TextoNormalizado.contiene(List.of("vela"), List.of());
    }

    private void cuandoSeArmanLasClavesDe(String nombre, String nombreEnSingular) {
        clave = TextoNormalizado.clave(nombre);
        claveDelSingular = TextoNormalizado.clave(nombreEnSingular);
    }

    // --- entonces ---
    private void entoncesElTextoNormalizadoEs(String esperado) {
        assertThat(normalizado).isEqualTo(esperado);
    }

    private void entoncesLasPalabrasSon(String... esperadas) {
        assertThat(palabras).containsExactly(esperadas);
    }

    private void entoncesLasPalabrasDelPluralYDelSingularSonLasMismas() {
        assertThat(palabras).isEqualTo(palabrasDelSingular);
    }

    private void entoncesNoHayPalabras() {
        assertThat(palabras).isEmpty();
    }

    private void entoncesLaFraseSeEncuentra() {
        assertThat(contiene).isTrue();
    }

    private void entoncesLaFraseNoSeEncuentra() {
        assertThat(contiene).isFalse();
    }

    private void entoncesLasClavesSonIguales() {
        assertThat(clave).isEqualTo(claveDelSingular);
    }
}
