package com.ensambla.backend.domain.model.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoincidenciaDeFilasTest {

    private static final List<String> PRODUCTOS = List.of("Harina 000", "Azúcar impalpable", "Torta de chocolate",
            "Dulce de leche");

    @Test
    @DisplayName("Encuentra el nombre exacto sin importar tildes ni mayúsculas")
    void buscarShouldIgnoreAccentsAndCase() {
        assertThat(CoincidenciaDeFilas.buscar("AZUCAR IMPALPABLE", PRODUCTOS)).contains(1);
    }

    @Test
    @DisplayName("Encuentra el producto si la persona escribió sólo parte del nombre")
    void buscarShouldMatchPartOfTheName() {
        assertThat(CoincidenciaDeFilas.buscar("harina", PRODUCTOS)).contains(0);
        assertThat(CoincidenciaDeFilas.buscar("torta chocolate", PRODUCTOS)).contains(2);
    }

    @Test
    @DisplayName("Tolera plural, singular y una letra mal escrita")
    void buscarShouldTolerateTyposAndPlurals() {
        assertThat(CoincidenciaDeFilas.buscar("tortas", PRODUCTOS)).contains(2);
        assertThat(CoincidenciaDeFilas.buscar("hariana", PRODUCTOS)).contains(0);
        assertThat(CoincidenciaDeFilas.buscar("dulse de leche", PRODUCTOS)).contains(3);
    }

    @Test
    @DisplayName("Una palabra que empieza igual que la del producto cuenta como la misma")
    void buscarShouldMatchByPrefix() {
        assertThat(CoincidenciaDeFilas.buscar("choco", PRODUCTOS)).contains(2);
    }

    @Test
    @DisplayName("También coincide cuando la persona escribió la palabra más larga que la de la base")
    void buscarShouldMatchWhenTheTypedWordIsLonger() {
        assertThat(CoincidenciaDeFilas.buscar("chocolate", List.of("Choco", "Pan"))).contains(0);
    }

    @Test
    @DisplayName("Si ningún producto se parece no elige ninguno")
    void buscarShouldReturnEmptyWhenNothingMatches() {
        assertThat(CoincidenciaDeFilas.buscar("manteca", PRODUCTOS)).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("de la", PRODUCTOS)).isEmpty();
    }

    @Test
    @DisplayName("Si dos productos parecen igual de buenos no elige ninguno, para no pisar el equivocado")
    void buscarShouldNotGuessBetweenEqualCandidates() {
        List<String> harinas = List.of("Harina 000", "Harina 0000", "Azúcar");

        assertThat(CoincidenciaDeFilas.buscar("harina", harinas)).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("harina 000", harinas)).contains(0);
    }

    @Test
    @DisplayName("Un texto vacío o sin candidatos no coincide con nada")
    void buscarShouldHandleEmptyInputs() {
        assertThat(CoincidenciaDeFilas.buscar("", PRODUCTOS)).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("harina", List.of())).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("harina", List.of(""))).isEmpty();
    }

    @Test
    @DisplayName("Una palabra corta con una letra de diferencia no se confunde con otra")
    void buscarShouldNotTreatShortWordsAsTypos() {
        assertThat(CoincidenciaDeFilas.buscar("sal", List.of("Sol", "Mal"))).isEmpty();
    }

    @Test
    @DisplayName("Encuentra el producto con la medida pegada al número, sin los «de» ni «la» y con letras de más")
    void buscarShouldMatchTheWayPeopleWriteOnWhatsapp() {
        List<String> productos = List.of("Leche La Serenísima 1 L", "Yerba Playadito 1 kg");

        assertThat(CoincidenciaDeFilas.buscar("leche la serenisima de 1 L", productos)).contains(0);
        assertThat(CoincidenciaDeFilas.buscar("leche serenisima 1l", productos)).contains(0);
        assertThat(CoincidenciaDeFilas.buscar("lechee serenissimaa", productos)).contains(0);
    }

    @Test
    @DisplayName("No confunde otra marca ni otra medida aunque compartan casi todo el nombre")
    void buscarShouldNotMatchAnotherBrandOrSize() {
        List<String> productos = List.of("Leche La Serenísima 1 L");

        assertThat(CoincidenciaDeFilas.buscar("leche sancor 1 l", productos)).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("leche serenisima 2 l", productos)).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("harina 500", List.of("Harina 5000"))).isEmpty();
    }

    @Test
    @DisplayName("Prefiere el nombre completo a uno que comparte sólo una parte")
    void buscarShouldPreferTheBestCandidate() {
        assertThat(CoincidenciaDeFilas.buscar("torta chocolate", List.of("Torta de chocolate", "Torta"))).contains(0);
    }

    @Test
    @DisplayName("Si sobra más de la mitad de lo que escribió la persona no coincide, aunque sean palabras cortas")
    void buscarShouldRejectWhenLessThanHalfMatches() {
        assertThat(CoincidenciaDeFilas.buscar("torta grande rica", List.of("Torta"))).isEmpty();
        assertThat(CoincidenciaDeFilas.buscar("pan l", List.of("Pan kg cm"))).isEmpty();
    }

    @Test
    @DisplayName("Con mucho en común pero no todo, coincide si comparten al menos la mitad de las palabras")
    void buscarShouldAcceptHalfOfTheWords() {
        assertThat(CoincidenciaDeFilas.buscar("torta chocolate grande", List.of("Torta chocolate", "Pan"))).contains(0);
    }
}
