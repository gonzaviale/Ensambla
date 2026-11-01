package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextoNormalizadoTest {

    @Test
    @DisplayName("Ignora tildes, mayúsculas y espacios de los bordes")
    void deShouldIgnoreAccentsCaseAndBorderSpaces() {
        assertThat(TextoNormalizado.de("  Juan PÉREZ ")).isEqualTo("juan perez");
    }

    @Test
    @DisplayName("Conserva los espacios y signos del medio")
    void deShouldKeepInnerCharacters() {
        assertThat(TextoNormalizado.de("Ñoquis  de papa-2")).isEqualTo("noquis  de papa-2");
    }

    @Test
    @DisplayName("Pasa a minúsculas, quita tildes y signos y separa en palabras")
    void palabrasShouldLowercaseStripAccentsAndPunctuation() {
        List<String> result = TextoNormalizado.palabras("  ¡Cobro, ANOTO: quién me DEBE!  ");

        assertThat(result).containsExactly("cobr", "anot", "quien", "me", "debe");
    }

    @Test
    @DisplayName("El singular y el plural dan la misma raíz")
    void palabrasShouldGiveSameRootForSingularAndPlural() {
        assertThat(TextoNormalizado.palabras("velas")).isEqualTo(TextoNormalizado.palabras("vela"));
        assertThat(TextoNormalizado.palabras("clientes")).isEqualTo(TextoNormalizado.palabras("cliente"));
        assertThat(TextoNormalizado.palabras("pedidos")).isEqualTo(TextoNormalizado.palabras("pedido"));
    }

    @Test
    @DisplayName("No recorta las palabras cortas ni las que no terminan en vocal o plural")
    void palabrasShouldKeepShortWordsAndConsonantEndings() {
        List<String> result = TextoNormalizado.palabras("me los dos stock luz");

        assertThat(result).containsExactly("me", "los", "dos", "stock", "luz");
    }

    @Test
    @DisplayName("Un texto vacío o sólo de signos no tiene palabras")
    void palabrasShouldBeEmptyWhenTextHasNoLetters() {
        assertThat(TextoNormalizado.palabras("")).isEmpty();
        assertThat(TextoNormalizado.palabras("¿¡...!?")).isEmpty();
    }

    @Test
    @DisplayName("Encuentra una frase completa y en orden dentro de las palabras")
    void contieneShouldFindPhraseInOrder() {
        List<String> palabras = TextoNormalizado.palabras("anoto quien me debe plata");

        assertThat(TextoNormalizado.contiene(palabras, TextoNormalizado.palabras("me debe"))).isTrue();
        assertThat(TextoNormalizado.contiene(palabras, TextoNormalizado.palabras("debe me"))).isFalse();
        assertThat(TextoNormalizado.contiene(palabras, TextoNormalizado.palabras("me deben mucho"))).isFalse();
    }

    @Test
    @DisplayName("Una frase vacía nunca coincide")
    void contieneShouldBeFalseForEmptyPhrase() {
        assertThat(TextoNormalizado.contiene(List.of("vela"), List.of())).isFalse();
    }

    @Test
    @DisplayName("La clave de un nombre ignora tildes, mayúsculas y plural")
    void claveShouldMatchNamesWrittenDifferently() {
        assertThat(TextoNormalizado.clave("Teléfonos")).isEqualTo(TextoNormalizado.clave("telefono"));
    }
}
