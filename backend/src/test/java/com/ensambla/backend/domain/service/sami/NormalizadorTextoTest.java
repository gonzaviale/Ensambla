package com.ensambla.backend.domain.service.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorTextoTest {

    @Test
    @DisplayName("Pasa a minúsculas, quita tildes y signos y separa en palabras")
    void palabrasShouldLowercaseStripAccentsAndPunctuation() {
        List<String> result = NormalizadorTexto.palabras("  ¡Cobro, ANOTO: quién me DEBE!  ");

        assertThat(result).containsExactly("cobr", "anot", "quien", "me", "debe");
    }

    @Test
    @DisplayName("El singular y el plural dan la misma raíz")
    void palabrasShouldGiveSameRootForSingularAndPlural() {
        assertThat(NormalizadorTexto.palabras("velas")).isEqualTo(NormalizadorTexto.palabras("vela"));
        assertThat(NormalizadorTexto.palabras("clientes")).isEqualTo(NormalizadorTexto.palabras("cliente"));
        assertThat(NormalizadorTexto.palabras("pedidos")).isEqualTo(NormalizadorTexto.palabras("pedido"));
    }

    @Test
    @DisplayName("No recorta las palabras cortas ni las que no terminan en vocal o plural")
    void palabrasShouldKeepShortWordsAndConsonantEndings() {
        List<String> result = NormalizadorTexto.palabras("me los dos stock luz");

        assertThat(result).containsExactly("me", "los", "dos", "stock", "luz");
    }

    @Test
    @DisplayName("Un texto vacío o sólo de signos no tiene palabras")
    void palabrasShouldBeEmptyWhenTextHasNoLetters() {
        assertThat(NormalizadorTexto.palabras("")).isEmpty();
        assertThat(NormalizadorTexto.palabras("¿¡...!?")).isEmpty();
    }

    @Test
    @DisplayName("Encuentra una frase completa y en orden dentro de las palabras")
    void contieneShouldFindPhraseInOrder() {
        List<String> palabras = NormalizadorTexto.palabras("anoto quien me debe plata");

        assertThat(NormalizadorTexto.contiene(palabras, NormalizadorTexto.palabras("me debe"))).isTrue();
        assertThat(NormalizadorTexto.contiene(palabras, NormalizadorTexto.palabras("debe me"))).isFalse();
        assertThat(NormalizadorTexto.contiene(palabras, NormalizadorTexto.palabras("me deben mucho"))).isFalse();
    }

    @Test
    @DisplayName("Una frase vacía nunca coincide")
    void contieneShouldBeFalseForEmptyPhrase() {
        assertThat(NormalizadorTexto.contiene(List.of("vela"), List.of())).isFalse();
    }
}
